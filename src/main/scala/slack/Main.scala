package slack

import akka.actor._
import slack.rtm.SlackRtmClient

object Main extends App {
  val token = "..."
  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher

  val client = SlackRtmClient(token)
  val selfId = client.state.self.id

  client.onEvent { event =>
    system.log.info("Received new event: {}", event)
    /*
    import models._
    event match {
      case message: Message => {
        val mentionedIds = SlackUtil.extractMentionedIds(message.text)

        if (mentionedIds.contains(selfId)) {
          client.sendMessage(message.channel, s"<@${message.user}>: Hey!")
        }
      }
      case _ => {}
    }
    */
  }
}
