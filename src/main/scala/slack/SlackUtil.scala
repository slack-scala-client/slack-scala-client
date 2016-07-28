package slack

import slack.models.Message

object SlackUtil {

  val mentionrx = """<@(\w+)>""".r

  def extractMentionedIds(text: String): Seq[String] = {
    mentionrx.findAllMatchIn(text).toVector.map(_.subgroups.head)
  }

  def isDirectMsg(m: Message): Boolean = {
    m.channel.startsWith("D")
  }
}