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
  case object WebSocketClientDisconnected
  case object WebSocketClientConnected
  case object WebSocketClientConnectFailed
  case object CheckPongSendPing

  val WEBSOCKET_TIMEOUT = 10000L

  def apply(url: String, listeners: Seq[ActorRef])(implicit arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props(new WebSocketClientActor(url, listeners)))
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

class WebSocketClientActor(url: String, initialListeners: Seq[ActorRef]) extends WebSocketClientWorker with ActorLogging {
  implicit val ec = context.dispatcher
  implicit val system = context.system

  val listeners = MSet[ActorRef](initialListeners: _*)
  val uri = new URI(url)
  log.info("[WebSocketClient] Connecting to RTM: {}", uri)
  IO(UHttp) ! Http.Connect(uri.getHost, if(uri.getPort > 0) uri.getPort else 443, true)

  var pingPongTask: Option[Cancellable] = None
  var lastPing: Option[Long] = None
  var lastPong: Option[Long] = None

  def upgradeRequest = HttpRequest(HttpMethods.GET, uri.getPath, websocketHeaders(uri.getHost, uri.getPort))

  override def receive: Receive = coreReceive orElse super.receive

  def businessLogic: Receive = coreReceive orElse {
    case CheckPongSendPing =>
      handlePingPongCheck()
    case frame: TextFrame =>
      log.debug("[WebSocketClientActor] Received Text Frame: {}", frame.payload.decodeString("utf8"))
      listeners.foreach(_ ! frame)
    case frame: PongFrame =>
      lastPong = Some(System.currentTimeMillis)
    case frame: Frame =>
      log.debug("[WebSocketClientActor] Received Frame: {}", frame)
    case SendFrame(frame) =>
      connection ! frame
    case UpgradedToWebSocket =>
      pingPongTask = Some(context.system.scheduler.schedule(1.second, 1.second, self, CheckPongSendPing))
      listeners.foreach(_ ! WebSocketClientConnected)
    case _: Http.ConnectionClosed =>
      log.info("[WebSocketClientActor] Websocket closed")
      context.stop(self)
  }

  def coreReceive: PartialFunction[Any, Unit] = {
    case Http.CommandFailed(con: Http.Connect) =>
      log.info("[WebSocketClientActor] Connection Failed")
      listeners.foreach(_ ! WebSocketClientConnectFailed)
    case RegisterWebsocketListener(listener) =>
      log.info("[WebSocketClientActor] Registering listener")
      listeners += listener
      context.watch(listener)
    case DeregisterWebsocketListener(listener) =>
      listeners -= listener
    case Terminated(actor) =>
      listeners -= actor
  }

  def handlePingPongCheck() {
    if(!pingPongInitialized) {
      initializePingPong()
    } else if(pongTimedOut) {
      context.stop(self)
    } else {
      sendPing()
    }
  }

  def pingPongInitialized: Boolean = lastPing.isDefined && lastPong.isDefined

  def initializePingPong() {
    connection ! PingFrame()
    lastPong = Some(System.currentTimeMillis)
    lastPing = Some(System.currentTimeMillis)
  }

  def pongTimedOut: Boolean = (System.currentTimeMillis - lastPong.get) > WEBSOCKET_TIMEOUT

  def sendPing() {
    connection ! PingFrame()
    lastPing = Some(System.currentTimeMillis)
  }

  override def postStop() {
    listeners.foreach(_ ! WebSocketClientDisconnected)
    pingPongTask.foreach(_.cancel)
    context.stop(connection)
  }
}