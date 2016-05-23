package slack.models

import play.api.libs.json._

case class Team (
  id: String,
  name: String,
  domain:String,
  email_domain: String,
  msg_edit_window_mins: Int,
  over_storage_limit: Boolean,
  prefs: JsValue,
  plan: String
)