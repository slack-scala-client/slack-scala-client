package slack.rtm

import java.net.{InetSocketAddress, URI}

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, Props}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.http.scaladsl.{ClientTransport, Http}
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.typesafe.config.ConfigFactory
import slack.rtm.WebSocketClientActor._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

private[rtm] object WebSocketClientActor {
  case class SendWSMessage(message: Message)
  case class RegisterWebsocketListener(listener: ActorRef)
  case class DeregisterWebsocketListener(listener: ActorRef)

  case object WebSocketClientConnected
  case object WebSocketClientDisconnected
  case object WebSocketClientConnectFailed

  case class WebSocketConnectSuccess(queue: SourceQueueWithComplete[Message], closed: Future[Done])
  case object WebSocketConnectFailure
  case object WebSocketDisconnected

  private[this] val config   = ConfigFactory.load()
  private[this] val useProxy: Boolean = Try(config.getString("slack-scala-client.http.useproxy"))
    .map(_.toBoolean)
    .recover{case _:Exception => false}.getOrElse(false)

  private[WebSocketClientActor] val maybeSettings: Option[ClientConnectionSettings] = if (useProxy) {
    val proxyHost = config.getString("slack-scala-client.http.proxyHost")
    val proxyPort = config.getString("slack-scala-client.http.proxyPort").toInt

    val httpsProxyTransport = ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(proxyHost, proxyPort))

    Some(ClientConnectionSettings(config)
        .withTransport(httpsProxyTransport))
  } else {
    None
  }

  def apply(url: String)(implicit arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props(new WebSocketClientActor(url)))
  }
}

private[rtm] class WebSocketClientActor(url: String) extends Actor with ActorLogging {
  implicit val ec = context.dispatcher
  implicit val system = context.system
  implicit val materalizer = ActorMaterializer()

  val uri = new URI(url)
  var outboundMessageQueue: Option[SourceQueueWithComplete[Message]] = None

  override def receive = {
    case m: TextMessage =>
      log.debug("[WebSocketClientActor] Received Text Message: {}", m)
      context.parent ! m
    case m: Message =>
      log.debug("[WebsocketClientActor] Received Message: {}", m)
    case SendWSMessage(m) =>
      outboundMessageQueue.map(_.offer(m))
    case WebSocketConnectSuccess(queue, closed) =>
      outboundMessageQueue = Some(queue)
      closed.onComplete(_ => self ! WebSocketDisconnected)
      context.parent ! WebSocketClientConnected
    case WebSocketDisconnected =>
      log.info("[WebSocketClientActor] WebSocket disconnected.")
      context.stop(self)
    case _ =>
  }

  def connectWebSocket(): Unit = {
    val messageSink: Sink[Message, Future[Done]] = {
      Sink.foreach {
        case message => self ! message
      }
    }

    val queueSource: Source[Message, SourceQueueWithComplete[Message]] = {
      Source.queue[Message](1000, OverflowStrategy.dropHead)
    }

    val flow: Flow[Message, Message, (Future[Done], SourceQueueWithComplete[Message])] =
      Flow.fromSinkAndSourceMat(messageSink, queueSource)(Keep.both)

    val (upgradeResponse, (closed, messageSourceQueue)) =
      Http().singleWebSocketRequest(request = WebSocketRequest(url),
        clientFlow = flow,
        settings = maybeSettings.getOrElse(ClientConnectionSettings(system)))

    upgradeResponse.onComplete {
      case Success(upgrade) if upgrade.response.status == StatusCodes.SwitchingProtocols =>
        log.info("[WebSocketClientActor] Web socket connection success")
        self ! WebSocketConnectSuccess(messageSourceQueue, closed)
      case Success(upgrade) =>
        log.info("[WebSocketClientActor] Web socket connection failed: {}", upgrade.response)
        context.parent ! WebSocketClientConnectFailed
        context.stop(self)
      case Failure(err) =>
        log.info("[WebSocketClientActor] Web socket connection failed with error: {}", err.getMessage)
        context.parent ! WebSocketClientConnectFailed
        context.stop(self)
    }
  }

  override def preStart(): Unit = {
    log.info("WebSocketClientActor] Connecting to RTM: {}", url)
    connectWebSocket()
  }

  override def postStop(): Unit = {
    outboundMessageQueue.foreach(_.complete)
    context.parent ! WebSocketClientDisconnected
  }
}
