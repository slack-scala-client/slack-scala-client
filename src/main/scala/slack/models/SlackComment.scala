package slack.models

case class SlackComment (
  id: String,
  tiemstamp: Long,
  user: String,
  comment: String
)