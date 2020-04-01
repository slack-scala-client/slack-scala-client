package slack

import slack.models.Message

object SlackUtil {

  private val mentionrx = """<@(\w+)>""".r

  def extractMentionedIds(text: String): Seq[String] = {
    mentionrx.findAllMatchIn(text).toVector.flatMap(_.subgroups.headOption)
  }

  def mentionsId(text: String, id: String): Boolean = {
    extractMentionedIds(text).contains(id)
  }

  def isDirectMsg(m: Message): Boolean = {
    m.channel.startsWith("D")
  }
}
