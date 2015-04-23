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
  def apply(token: String)(implicit arf: ActorRefFactory): SlackRtmClient = {
    new SlackRtmClient(token)
  }
}

class SlackRtmClient(token: String)(implicit arf: ActorRefFactory) {
  implicit val timeout = new Timeout(5.second)

  val actor = SlackRtmConnectionActor(token)
  val state = Await.result((actor ? StateRequest()).mapTo[StateResponse], 5.seconds).state

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
}

object SlackRtmConnectionActor {

  implicit val sendMessageFmt = Json.format[MessageSend]

  case class AddEventListener(listener: ActorRef)
  case class RemoveEventListener(listener: ActorRef)
  case class SendMessage(channelId: String, text: String)
  case class StateRequest()
  case class StateResponse(state: RtmState)

  def apply(token: String)(implicit arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props(new SlackRtmConnectionActor(token)))
  }
}

class SlackRtmConnectionActor(token: String) extends Actor with ActorLogging {

  implicit val ec = context.dispatcher
  val apiClient = BlockingSlackApiClient(token)
  val initialRtmState = apiClient.startRealTimeMessageSession()
  val rtmState = RtmState(initialRtmState)
  val webSocketClient = WebSocketClientActor(initialRtmState.url)
  val listeners = MSet[ActorRef]()
  val idCounter = new AtomicLong(1L)

  def receive = {
    case frame: TextFrame =>
      val payload = frame.payload.decodeString("utf8")
      val payloadJson = Json.parse(payload)
      if((payloadJson \ "type").asOpt[String].isDefined){
        val event = payloadJson.as[SlackEvent]
        rtmState.update(event)
        listeners.foreach(_ ! event)
      } else {
        // TODO: handle reply_to / response
      }
    case SendMessage(channelId, text) =>
      val nextId = idCounter.getAndIncrement
      val payload = Json.stringify(Json.toJson(MessageSend(nextId, channelId, text)))
      webSocketClient ! SendFrame(TextFrame(ByteString(payload)))
    case StateRequest() =>
      sender ! StateResponse(rtmState)
    case AddEventListener(listener) =>
      listeners += listener
      context.watch(listener)
    case RemoveEventListener(listener) =>
      listeners -= listener
    case Terminated(actor) =>
      listeners -= actor
    case _ =>
  }

  override def preStart() {
    log.info("[SlackRtmConnectionActor] Registering with web socket client")
    webSocketClient ! RegisterWebsocketListener(self)
  }

  override def postStop() {
    context.stop(webSocketClient)
  }
}

case class MessageSend(id: Long, channel: String, text: String, `type`: String = "message")