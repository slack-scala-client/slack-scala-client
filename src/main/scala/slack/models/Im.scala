package slack.models

case class Im (
  id: String,
  is_im: Boolean,
  user: String,
  created: Long,
  is_user_deleted: Option[Boolean]
)