package slack.rtm

import slack.api._
import slack.models._
import slack.rtm.SlackRtmConnectionActor._
import slack.rtm.WebSocketClientActor._

import java.util.concurrent.atomic.AtomicLong
import scala.collection.mutable.{Set => MSet}
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import play.api.libs.json._
import akka.http.scaladsl.model.ws.TextMessage

object SlackRtmClient {
  def apply(token: String, duration: FiniteDuration = 5.seconds)(implicit arf: ActorSystem): SlackRtmClient = {
    new SlackRtmClient(token, duration)
  }
}

class SlackRtmClient(token: String, duration: FiniteDuration = 5.seconds)(implicit arf: ActorSystem) {
  private implicit val timeout = new Timeout(duration)

  private val apiClient = BlockingSlackApiClient(token, duration)
  val state = RtmState(apiClient.startRealTimeMessageSession())
  private val actor = SlackRtmConnectionActor(token, state, duration)

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

  def sendMessage(channelId: String, text: String, thread_ts: Option[String] = None): Future[Long] = {
    (actor ? SendMessage(channelId, text, thread_ts)).mapTo[Long]
  }

  def editMessage(channelId: String, ts: String, text: String) {
    actor ! BotEditMessage(channelId, ts, text)
  }

  def indicateTyping(channel: String) {
    actor ! TypingMessage(channel)
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

private[rtm] object SlackRtmConnectionActor {

  implicit val sendMessageFmt = Json.format[MessageSend]
  implicit val botEditMessageFmt = Json.format[BotEditMessage]
  implicit val typingMessageFmt = Json.format[MessageTyping]

  case class AddEventListener(listener: ActorRef)
  case class RemoveEventListener(listener: ActorRef)
  case class SendMessage(channelId: String, text: String, ts_thread: Option[String] = None)
  case class BotEditMessage(channelId: String, ts: String, text: String, as_user: Boolean = true, `type`:String = "chat.update")
  case class TypingMessage(channelId: String)
  case class StateRequest()
  case class StateResponse(state: RtmState)
  case object ReconnectWebSocket

  def apply(token: String, state: RtmState, duration: FiniteDuration)(implicit arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props(new SlackRtmConnectionActor(token, state, duration)))
  }
}

private[rtm] class SlackRtmConnectionActor(token: String, state: RtmState, duration: FiniteDuration) extends Actor with ActorLogging {

  implicit val ec = context.dispatcher
  implicit val system = context.system
  val apiClient = BlockingSlackApiClient(token, duration)
  val listeners = MSet[ActorRef]()
  val idCounter = new AtomicLong(1L)

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: Exception => SupervisorStrategy.Stop
    }

  var connectFailures = 0
  var webSocketClient: Option[ActorRef] = None

  def receive = {
    case message: TextMessage =>
      try {
        val payload = message.getStrictText
        val payloadJson = Json.parse(payload)
        if ((payloadJson \ "type").asOpt[String].isDefined || (payloadJson \ "reply_to").asOpt[Long].isDefined) {
          Try(payloadJson.as[SlackEvent]) match {
            case Success(event) =>
              state.update(event)
              listeners.foreach(_ ! event)
            case Failure(e) => log.error(e, s"[SlackRtmClient] Error reading event: $payload")
          }
        } else {
          log.warning(s"invalid slack event : $payload")
        }
      } catch {
        case e: Exception => log.error(e, "[SlackRtmClient] Error parsing text message")
      }
    case TypingMessage(channelId) =>
      val nextId = idCounter.getAndIncrement
      val payload = Json.stringify(Json.toJson(MessageTyping(nextId, channelId)))
      webSocketClient.get ! SendWSMessage(TextMessage(payload))
    case SendMessage(channelId, text, ts_thread) =>
      val nextId = idCounter.getAndIncrement
      val payload = Json.stringify(Json.toJson(MessageSend(nextId, channelId, text, ts_thread)))
      webSocketClient.get ! SendWSMessage(TextMessage(payload))
      sender ! nextId
    case bm: BotEditMessage =>
      val payload = Json.stringify(Json.toJson(bm))
      webSocketClient.get ! SendWSMessage(TextMessage(payload))
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
    case WebSocketClientDisconnected =>
      handleWebSocketDisconnect(sender)
    case WebSocketClientConnectFailed =>
      val delay = Math.pow(2.0, connectFailures.toDouble).toInt
      log.info("[SlackRtmConnectionActor] WebSocket Client failed to connect, retrying in {} seconds", delay)
      connectFailures += 1
      webSocketClient = None
      context.system.scheduler.scheduleOnce(delay.seconds, self, ReconnectWebSocket)
    case ReconnectWebSocket =>
      connectWebSocket()
    case Terminated(actor) =>
      listeners -= actor
      handleWebSocketDisconnect(actor)
    case _ =>
      log.warning("doesn't match any case, skip")
  }

  def connectWebSocket() {
    log.info("[SlackRtmConnectionActor] Starting web socket client")
    try {
      val initialRtmState = apiClient.startRealTimeMessageSession()
      state.reset(initialRtmState)
      webSocketClient = Some(WebSocketClientActor(initialRtmState.url)(context))
      webSocketClient.foreach(context.watch)
    } catch {
      case e: Exception =>
        log.error(e, "Caught exception trying to connect websocket")
        self ! WebSocketClientConnectFailed
    }
  }

  def handleWebSocketDisconnect(actor: ActorRef) {
    if (webSocketClient.isDefined && webSocketClient.get == actor) {
      log.info("[SlackRtmConnectionActor] WebSocket Client disconnected, reconnecting")
      webSocketClient.foreach(context.stop)
      connectWebSocket()
    }
  }

  override def preStart() {
    connectWebSocket()
  }

  override def postStop() {
    webSocketClient.foreach(context.stop)
  }
}

private[rtm] case class MessageSend(id: Long, channel: String, text: String, thread_ts: Option[String] = None, `type`: String = "message")
private[rtm] case class MessageTyping(id: Long, channel: String, `type`: String = "typing")
