package slack.models

case class User (
  id: String,
  name: String,
  deleted: Option[Boolean],
  color: Option[String],
  profile: Option[UserProfile],
  is_bot: Option[Boolean],
  is_admin: Option[Boolean],
  is_owner: Option[Boolean],
  is_primary_owner: Option[Boolean],
  is_restricted: Option[Boolean],
  is_ultra_restricted: Option[Boolean],
  has_2fa: Option[Boolean],
  has_files: Option[Boolean],
  tz: Option[String],
  tz_offset: Option[Int],
  presence: Option[String]
)

case class UserProfile (
  first_name: Option[String],
  last_name: Option[String],
  real_name: Option[String],
  email: Option[String],
  skype: Option[String],
  phone: Option[String],
  image_24: String,
  image_32: String,
  image_48: String,
  image_72: String,
  image_192: String
)