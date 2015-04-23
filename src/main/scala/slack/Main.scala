package slack

import slack.api.SlackApiClient
import slack.rtm.SlackRtmClient
import scala.util.{Success,Failure}
import akka.actor._

object Main extends App {
  val token = "..."
  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher

  val rtmClient = SlackRtmClient(token)
  rtmClient.onMessage { message =>
    system.log.info("User: {}, Message: {}", message.user, message.text)
  }
}