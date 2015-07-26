package slack.rtm

import slack.api._
import slack.models._

import scala.concurrent._
import scala.collection.mutable.{Set => MSet}
import scala.concurrent.duration._
import java.util.concurrent.atomic.AtomicLong

import akka.actor._
import akka.util.{ByteString,Timeout}
import akka.pattern.ask
import play.api.libs.json._
import spray.can.websocket.frame._

import WebSocketClientActor._
import SlackRtmConnectionActor._

object SlackRtmClient {
  def apply(token: String, duration: FiniteDuration = 5.seconds)(implicit arf: ActorRefFactory): SlackRtmClient = {
    new SlackRtmClient(token, duration)
  }
}

class SlackRtmClient(token: String, duration: FiniteDuration = 5.seconds)(implicit arf: ActorRefFactory) {
  implicit val timeout = new Timeout(duration)
  implicit val ec = arf.dispatcher

  val apiClient = BlockingSlackApiClient(token, duration)
  val state = RtmState(apiClient.startRealTimeMessageSession())
  val actor = SlackRtmConnectionActor(token, state)

  def onEvent(f: (SlackEvent) => Unit): ActorRef = {
    val handler = EventHandlerActor(f)
    addEventListener(handler)
    handler
  }

  def onMessage(f: (Message) => Unit): ActorRef = {
    val handler = MessageHandlerActor(f)
    addEventListener(handler)
    handler
  }

  def sendMessage(channelId: String, text: String) {
    actor ! SendMessage(channelId, text)
  }

  def indicateTyping(channel: String) {
    ???
  }

  def addEventListener(listener: ActorRef) {
    actor ! AddEventListener(listener)
  }

  def removeEventListener(listener: ActorRef) {
    actor ! RemoveEventListener(listener)
  }

  def getState(): RtmState = {
    state
  }

  def close() {
    arf.stop(actor)
  }
}

object SlackRtmConnectionActor {

  implicit val sendMessageFmt = Json.format[MessageSend]

  case class AddEventListener(listener: ActorRef)
  case class RemoveEventListener(listener: ActorRef)
  case class SendMessage(channelId: String, text: String)
  case class StateRequest()
  case class StateResponse(state: RtmState)
  case object ReconnectWebSocket

  def apply(token: String, state: RtmState)(implicit arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props(new SlackRtmConnectionActor(token, state)))
  }
}

class SlackRtmConnectionActor(token: String, state: RtmState) extends Actor with ActorLogging {

  implicit val ec = context.dispatcher
  val apiClient = BlockingSlackApiClient(token)
  val listeners = MSet[ActorRef]()
  val idCounter = new AtomicLong(1L)

  var connectFailures = 0
  var webSocketClient: Option[ActorRef] = None

  def receive = {
    case frame: TextFrame =>
      try {
        val payload = frame.payload.decodeString("utf8")
        val payloadJson = Json.parse(payload)
        if((payloadJson \ "type").asOpt[String].isDefined){
          val event = payloadJson.as[SlackEvent]
          state.update(event)
          listeners.foreach(_ ! event)
        } else {
          // TODO: handle reply_to / response
        }
      } catch {
        case e: Exception =>
          log.error(e, "[SlackRtmClient] Error parsing text frame")
      }
    case SendMessage(channelId, text) =>
      val nextId = idCounter.getAndIncrement
      val payload = Json.stringify(Json.toJson(MessageSend(nextId, channelId, text)))
      webSocketClient.get ! SendFrame(TextFrame(ByteString(payload)))
    case StateRequest() =>
      sender ! StateResponse(state)
    case AddEventListener(listener) =>
      listeners += listener
      context.watch(listener)
    case RemoveEventListener(listener) =>
      listeners -= listener
    case WebSocketClientConnected =>
      log.info("[SlackRtmConnectionActor] WebSocket Client successfully connected")
      connectFailures = 0
    case WebSocketClientConnectFailed =>
      val delay = Math.pow(2.0, connectFailures.toDouble).toInt
      log.info("[SlackRtmConnectionActor] WebSocket Client failed to connect, retrying in {} seconds", delay)
      connectFailures += 1
      context.system.scheduler.scheduleOnce(delay.seconds, self, ReconnectWebSocket)
    case WebSocketClientDisconnected =>
      log.info("[SlackRtmConnectionActor] WebSocket Client disconnected, reconnecting")
      connectWebSocket()
    case ReconnectWebSocket =>
      connectWebSocket()
    case Terminated(actor) =>
      listeners -= actor
    case _ =>
  }

  def connectWebSocket() {
    log.info("[SlackRtmConnectionActor] Starting web socket client")
    try {
      val initialRtmState = apiClient.startRealTimeMessageSession()
      state.reset(initialRtmState)
      webSocketClient = Some(WebSocketClientActor(initialRtmState.url, Seq(self)))
    } catch {
      case e: Exception =>
        log.error(e, "Caught exception trying to connect websocket")
        self ! WebSocketClientConnectFailed
    }
  }

  override def preStart() {
    connectWebSocket()
  }

  override def postStop() {
    webSocketClient.foreach(context.stop)
  }
}

case class MessageSend(id: Long, channel: String, text: String, `type`: String = "message")