package slack

import slack.api.SlackApiClient
import slack.rtm.SlackRtmClient
import akka.actor._

object Main extends App {
  val token = "..."
  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher

  val client = SlackRtmClient(token)
  val selfId = client.state.getSelfId()

  client.onMessage { message =>
    val mentionedIds = SlackUtil.extractMentionedIds(message.text)

    if(mentionedIds.contains(selfId)) {
      client.sendMessage(message.channel, s"<@${message.user}>: Hey!")
    }
  }
}