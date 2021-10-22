package slack.models

import play.api.libs.json._

case class Team(id: String,
                name: String,
                domain: String,
                email_domain: String,
                msg_edit_window_mins: Int,
                over_storage_limit: Boolean,
                prefs: JsValue,
                plan: String)

case class Subteam(id: String,
                   team_id: String,
                   is_usergroup: Boolean,
                   is_subteam: Boolean,
                   name: String,
                   description: String,
                   handle: String,
                   is_external: Boolean,
                   date_create: Long,
                   date_update: Long,
                   date_delete: Long,
                   // TODO auto_type ???
                   auto_provision: Boolean,
                   enterprise_subteam_id: String,
                   created_by: String,
                   updated_by: Option[String],
                   deleted_by: Option[String],
                   // TODO prefs: channels, groups
                   users: Option[List[String]],
                   user_count: Int
                  )
