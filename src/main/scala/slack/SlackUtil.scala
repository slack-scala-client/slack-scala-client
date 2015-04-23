package slack

object SlackUtil {

  val mentionrx = """<@(\w+)>""".r

  def extractMentionedIds(text: String): Seq[String] = {
    mentionrx.findAllMatchIn(text).toVector.map(_.subgroups.head)
  }
}