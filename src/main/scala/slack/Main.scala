package slack

import slack.api.SlackApiClient
import slack.rtm.SlackRtmClient
import akka.actor._

object Main extends App {
  val token = "xoxb-4519113137-pin7xf7s5CI0wxeQHltVEfkr"
  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher

  val client = SlackRtmClient(token)
  val selfId = client.state.self.id

  client.onEvent { event =>
    system.log.info("Received new event: {}", event)
    /*
    val mentionedIds = SlackUtil.extractMentionedIds(message.text)

    if(mentionedIds.contains(selfId)) {
      client.sendMessage(message.channel, s"<@${message.user}>: Hey!")
    }
    */
  }
}