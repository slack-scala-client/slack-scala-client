package slack.rtm

import slack.api._
import slack.models._

import scala.collection.mutable.{Set => MSet}
import akka.actor._
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
  val actor = SlackRtmConnectionActor(token)

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

  def sendMessage(channel: String, text: String) {
    ???
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
}

object SlackRtmConnectionActor {

  case class AddEventListener(listener: ActorRef)
  case class RemoveEventListener(listener: ActorRef)

  def apply(token: String)(implicit arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props(new SlackRtmConnectionActor(token)))
  }
}

class SlackRtmConnectionActor(token: String) extends Actor with ActorLogging {

  implicit val ec = context.dispatcher
  val apiClient = BlockingSlackApiClient(token)
  val rtmState = apiClient.startRealTimeMessageSession()
  val webSocketClient = WebSocketClientActor(rtmState.url)
  val listeners = MSet[ActorRef]()

  def receive = {
    case frame: TextFrame =>
      val payload = frame.payload.decodeString("utf8")
      val event = Json.parse(payload).as[SlackEvent]
      listeners.foreach(_ ! event)
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

object EventHandlerActor {
  def apply(f: (SlackEvent) => Unit)(implicit arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props(new EventHandlerActor(f)))
  }
}

class EventHandlerActor(f: (SlackEvent) => Unit) extends Actor with ActorLogging {
  def receive = {
    case e: SlackEvent =>
      try {
        f(e)
      } catch {
        case e: Exception =>
          log.error(e, "Caught exception in event handler")
      }
    case _ =>
  }
}

object MessageHandlerActor {
  def apply(f: (Message) => Unit)(implicit arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props(new MessageHandlerActor(f)))
  }
}

class MessageHandlerActor(f: (Message) => Unit) extends Actor with ActorLogging {
  def receive = {
    case m: Message =>
      try {
        f(m)
      } catch {
        case e: Exception =>
          log.error(e, "Caught exception in message handler")
      }
    case _ =>
  }
}