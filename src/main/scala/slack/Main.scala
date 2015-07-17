package slack

import akka.actor._
import slack.rtm.SlackRtmClient

import scala.concurrent.duration._

object Main extends App {
  val token = "..."
  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher

  val client = SlackRtmClient(token, 5.seconds)
  val selfId = client.state.self.id

  client.onMessage { message =>
    val mentionedIds = SlackUtil.extractMentionedIds(message.text)

    if (mentionedIds.contains(selfId)) {
      client.sendMessage(message.channel, s"<@${message.user}>: Hey!")
    }
  }
}