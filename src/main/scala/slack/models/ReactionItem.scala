package slack.models

trait ReactionItem

case class ReactionItemMessage(channel: String, ts: String) extends ReactionItem

case class ReactionItemFile(file: String) extends ReactionItem

case class ReactionItemFileComment(file: String, file_comment: String) extends ReactionItem