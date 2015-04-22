package slack.models

case class User (
  id: String,
  name: String,
  deleted: Boolean,
  color: String,
  profile: UserProfile,
  is_admin: Boolean,
  is_owner: Boolean,
  is_primary_owner: Boolean,
  is_restricted: Boolean,
  is_ultra_restricted: Boolean,
  has_2fa: Boolean,
  has_files: Boolean
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