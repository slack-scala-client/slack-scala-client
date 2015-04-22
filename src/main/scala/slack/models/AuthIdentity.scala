package slack.models

case class AuthIdentity (
  url: String,
  team: String,
  user: String,
  team_id: String,
  user_id: String
)