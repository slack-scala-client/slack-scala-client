package slack.rtm

import org.apache.pekko.actor._
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.model.ws.TextMessage
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout
import play.api.libs.json._
import slack.api._
import slack.models._
import slack.rtm.SlackRtmConnectionActor._
import slack.rtm.WebSocketClientActor._

import java.util.concurrent.atomic.AtomicLong
import scala.collection.mutable.{Set => MSet}
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object SlackRtmClient {
  def apply(token: String,
            slackApiBaseUri: Uri = SlackApiClient.defaultSlackApiBaseUri,
            duration: FiniteDuration = 5.seconds)(implicit arf: ActorSystem): SlackRtmClient = {
    new SlackRtmClient(token, slackApiBaseUri, duration)
  }
}

class SlackRtmClient(token: String, slackApiBaseUri: Uri, duration: FiniteDuration)(
  implicit arf: ActorSystem
) {
  private implicit val timeout: Timeout = new Timeout(duration)

  val apiClient = BlockingSlackApiClient(token, slackApiBaseUri, duration)
  val state = RtmState(apiClient.startRealTimeMessageSession())
  private val actor = createConnectionActor

  def createConnectionActor: ActorRef = {
    SlackRtmConnectionActor(apiClient, state)
  }

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

  def sendMessage(channelId: String,
                  text: String,
                  thread_ts: Option[String] = None,
                  unfurl_links: Option[Boolean] = None,
                  unfurl_media: Option[Boolean] = None): Future[Long] = {
    (actor ? SendMessage(channelId, text, thread_ts, unfurl_links, unfurl_media)).mapTo[Long]
  }

  def editMessage(channelId: String, ts: String, text: String): Unit = {
    actor ! BotEditMessage(channelId, ts, text)
  }

  def indicateTyping(channel: String): Unit = {
    actor ! TypingMessage(channel)
  }

  def addEventListener(listener: ActorRef): Unit = {
    actor ! AddEventListener(listener)
  }

  def removeEventListener(listener: ActorRef): Unit = {
    actor ! RemoveEventListener(listener)
  }

  def getState(): RtmState = {
    state
  }

  def close(): Unit = {
    arf.stop(actor)
  }
}

private[rtm] object SlackRtmConnectionActor {

  implicit val sendMessageFmt: Format[MessageSend] = Json.format[MessageSend]
  implicit val botEditMessageFmt: Format[BotEditMessage] = Json.format[BotEditMessage]
  implicit val typingMessageFmt: Format[MessageTyping] = Json.format[MessageTyping]
  implicit val pingMessageFmt: Format[Ping] = Json.format[Ping]

  case class AddEventListener(listener: ActorRef)
  case class RemoveEventListener(listener: ActorRef)
  case class SendMessage(channelId: String,
                         text: String,
                         ts_thread: Option[String] = None,
                         unfurl_links: Option[Boolean] = None,
                         unfurl_media: Option[Boolean] = None)
  case class BotEditMessage(channelId: String,
                            ts: String,
                            text: String,
                            as_user: Boolean = true,
                            `type`: String = "chat.update")
  case class TypingMessage(channelId: String)
  case object StateRequest
  case class StateResponse(state: RtmState)
  case object ReconnectWebSocket
  case object SendPing

  def apply(apiClient: BlockingSlackApiClient, state: RtmState)(implicit arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props(new SlackRtmConnectionActor(apiClient, state)))
  }
}

class SlackRtmConnectionActor(apiClient: BlockingSlackApiClient, state: RtmState)
    extends Actor
    with ActorLogging {

  implicit val ec: ExecutionContextExecutor = context.dispatcher
  implicit val system: ActorSystem = context.system
  val listeners = MSet[ActorRef]()
  val idCounter = new AtomicLong(1L)

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute, loggingEnabled = true) {
      case _: Exception => SupervisorStrategy.Restart
    }

  var connectFailures = 0
  var webSocketClient: Option[ActorRef] = None

  context.system.scheduler.scheduleWithFixedDelay(1.minute, 1.minute, self, SendPing)

  def receive = {
    case message: TextMessage =>
      onTextMessageReceive(message)
    case TypingMessage(channelId) =>
      val nextId = idCounter.getAndIncrement
      val payload = Json.stringify(Json.toJson(MessageTyping(nextId, channelId)))
      webSocketClient.foreach(_ ! SendWSMessage(TextMessage(payload)))
    case SendMessage(channelId, text, ts_thread, unfurl_links, unfurl_media) =>
      val nextId = idCounter.getAndIncrement
      val payload = Json.stringify(Json.toJson(MessageSend(nextId, channelId, text, ts_thread, unfurl_links, unfurl_media)))
      webSocketClient.foreach(_ ! SendWSMessage(TextMessage(payload)))
      sender() ! nextId
    case bm: BotEditMessage =>
      val payload = Json.stringify(Json.toJson(bm))
      webSocketClient.foreach(_ ! SendWSMessage(TextMessage(payload)))
    case StateRequest =>
      sender() ! StateResponse(state)
    case AddEventListener(listener) =>
      listeners += listener
      context.watch(listener)
    case RemoveEventListener(listener) =>
      listeners -= listener
    case WebSocketClientConnected =>
      log.info("[SlackRtmConnectionActor] WebSocket Client successfully connected")
      connectFailures = 0
    case WebSocketClientDisconnected =>
      handleWebSocketDisconnect(sender())
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
    case SendPing =>
      val nextId = idCounter.getAndIncrement
      val payload = Json.stringify(Json.toJson(Ping(nextId)))
      webSocketClient.map(_ ! SendWSMessage(TextMessage(payload)))
    case x =>
      log.warning(s"$x doesn't match any case, skip")
  }

  def onTextMessageReceive(message: TextMessage) = {
    try {
      val payload = message.getStrictText
      val payloadJson = Json.parse(payload)
      if ((payloadJson \ "type").asOpt[String].isDefined || (payloadJson \ "reply_to").asOpt[Long].isDefined) {
        Try(payloadJson.as[SlackEvent]) match {
          case Success(event) =>
            listeners.foreach(_ ! event)
          case Failure(e) => log.error(e, s"[SlackRtmClient] Error reading event: $payload")
        }
      } else {
        log.warning(s"invalid slack event : $payload")
      }
    } catch {
      case e: Exception => log.error(e, "[SlackRtmClient] Error parsing text message")
    }
  }

  def connectWebSocket(): Unit = {
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

  def handleWebSocketDisconnect(actor: ActorRef): Unit = {
    if (webSocketClient.contains(actor)) {
      log.info("[SlackRtmConnectionActor] WebSocket Client disconnected, reconnecting")
      webSocketClient.foreach(context.stop)
      connectWebSocket()
    }
  }

  override def preStart(): Unit = {
    connectWebSocket()
  }

  override def postStop(): Unit = {
    webSocketClient.foreach(context.stop)
  }
}

private[rtm] case class MessageSend(id: Long,
                                    channel: String,
                                    text: String,
                                    thread_ts: Option[String] = None,
                                    unfurl_links: Option[Boolean] = None,
                                    unfurl_media: Option[Boolean] = None,
                                    `type`: String = "message")
private[rtm] case class MessageTyping(id: Long, channel: String, `type`: String = "typing")
private[rtm] case class Ping(id: Long, `type`: String = "ping")
