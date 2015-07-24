package slack.models

case class Reaction (
  name: String,
  users: Seq[String],
  count: Int
)