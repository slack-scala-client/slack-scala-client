package slack.models

case class SlackComment (
  id: String,
  timestamp: Long,
  user: String,
  comment: String
)