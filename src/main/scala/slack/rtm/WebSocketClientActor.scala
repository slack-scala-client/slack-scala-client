package slack.rtm

import java.net.URI
import scala.collection.mutable.{Set => MSet}
import scala.concurrent.duration._

import akka.actor._
import akka.io.IO
import spray.can.Http
import spray.can.server.UHttp
import spray.can.websocket._
import spray.can.websocket.frame._
import spray.http.{ HttpHeaders, HttpMethods, HttpRequest }

object WebSocketClientActor {

  case class SendFrame(frame: Frame)
  case class RegisterWebsocketListener(listener: ActorRef)
  case class DeregisterWebsocketListener(listener: ActorRef)

  def apply(url: String)(implicit arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props(new WebSocketClientActor(url)))
  }

  def websocketHeaders(host: String, port: Int) = List (
    HttpHeaders.Host(host, if(port > 0) port else 443),
    HttpHeaders.Connection("Upgrade"),
    HttpHeaders.RawHeader("Upgrade", "websocket"),
    HttpHeaders.RawHeader("Sec-WebSocket-Version", "13"),
    HttpHeaders.RawHeader("Sec-WebSocket-Key", "x3JJHMbDL1EzLkh9GBhXDw==")
  )
}

import WebSocketClientActor._

class WebSocketClientActor(url: String) extends WebSocketClientWorker with ActorLogging {
  implicit val ec = context.dispatcher
  implicit val system = context.system

  val listeners = MSet[ActorRef]()
  val uri = new URI(url)
  log.info("[WebSocketClient] Connecting to RTM: {}", uri)
  IO(UHttp) ! Http.Connect(uri.getHost, if(uri.getPort > 0) uri.getPort else 443, true)

  def upgradeRequest = HttpRequest(HttpMethods.GET, uri.getPath, websocketHeaders(uri.getHost, uri.getPort))

  override def receive: Receive = super.receive orElse listenerReceive

  def businessLogic: Receive = listenerReceive orElse {
    case frame: TextFrame =>
      log.info("[WebSocketClient] Received Text Frame: {}", frame.payload.decodeString("utf8"))
      listeners.foreach(_ ! frame)
    case frame: Frame =>
      log.info("[WebSocketClient] Received Frame: {}", frame.payload.decodeString("utf8"))
    case SendFrame(frame) =>
      connection ! frame
    case _: Http.ConnectionClosed =>
      log.info("[WebSocketClient] Websocket closed")
      context.stop(self)
  }

  def listenerReceive: PartialFunction[Any, Unit] = {
    case RegisterWebsocketListener(listener) =>
      log.info("[WebSocketClientActor] Registring listener")
      listeners += listener
      context.watch(listener)
    case DeregisterWebsocketListener(listener) =>
      listeners -= listener
    case Terminated(actor) =>
      listeners -= actor
  }
}