package slack.models

import play.api.libs.json._

// TODO: Revisit all event objects (some are partial? - specifically channel)
sealed trait SlackEvent

case class Hello (
  `type`: String
) extends SlackEvent

// TODO: Message Sub-types
case class Message (
  ts: String,
  channel: String,
  user: String,
  text: String,
  is_starred: Option[Boolean],
  thread_ts: Option[String]
) extends SlackEvent

case class EditMessage (
  user: String,
  text: String,
  ts:String
)

case class MessageChanged (
  message: EditMessage,
  previous_message: EditMessage,
  ts: String,
  event_ts: String,
  channel: String
) extends SlackEvent

case class MessageDeleted (
  ts: String,
  deleted_ts: String,
  event_ts: String,
  channel: String
) extends SlackEvent

case class BotMessage (
  ts: String,
  channel: String,
  text: String,
  bot_id: String,
  username: Option[String],
  attachments: Option[Seq[Attachment]]
) extends SlackEvent

// TODO: Message Sub-types
case class MessageWithSubtype (
 ts: String,
 channel: String,
 user: String,
 text: String,
 is_starred: Option[Boolean],
 messageSubType: MessageSubtype
) extends SlackEvent

sealed trait MessageSubtype {
  def subtype: String
}

object MessageSubtypes {

  // Fallback for unhandled message sub-types
  case class UnhandledSubtype(subtype: String) extends MessageSubtype

  case class MeMessage(subtype: String) extends MessageSubtype {
    //val subtype = "me_message"
  }

  case class ChannelNameMessage(
    oldname: String,
    name: String
  ) extends MessageSubtype {
    val subtype = "channel_name"
  }

  case class FileShareMessage(
    file: SlackFile
  ) extends MessageSubtype {
    val subtype = "file_share"
  }
}

case class ReactionAdded (
  reaction: String,
  item: ReactionItem,
  event_ts: String,
  user: String,
  item_user: String
) extends SlackEvent

case class ReactionRemoved (
  reaction: String,
  item: ReactionItem,
  event_ts: String,
  user: String,
  item_user: String
) extends SlackEvent

case class UserTyping (
  channel: String,
  user: String
) extends SlackEvent

case class ChannelMarked (
  channel: String,
  ts: String
) extends SlackEvent

case class ChannelCreated (
  channel: Channel
) extends SlackEvent

case class ChannelJoined (
  channel: Channel
) extends SlackEvent

case class ChannelLeft (
  channel: String
) extends SlackEvent

case class ChannelDeleted (
  channel: String
) extends SlackEvent

case class ChannelRename (
  channel: Channel
) extends SlackEvent

case class ChannelArchive (
  channel: String,
  user: String
) extends SlackEvent

case class ChannelUnarchive (
  channel: String,
  user: String
) extends SlackEvent

case class ChannelHistoryChanged (
  latest: Long,
  ts: String,
  event_ts: String
) extends SlackEvent

case class ImCreated (
  user: String,
  channel: Im
) extends SlackEvent

case class ImOpened (
  user: String,
  channel: String
) extends SlackEvent

case class ImClose (
  user: String,
  channel: String
) extends SlackEvent

case class ImMarked (
  channel: String,
  ts: String
) extends SlackEvent

case class ImHistoryChanged (
  latest: Long,
  ts: String,
  event_ts: String
) extends SlackEvent

case class GroupJoined(
  channel: Channel
) extends SlackEvent

case class MpImJoined(
  channel: Channel
) extends SlackEvent

case class MpImOpen(
  user: String,
  channel: String,
  event_ts: String
) extends SlackEvent

case class MpImClose(
  user: String,
  channel: String,
  event_ts: String,
  converted_to: Option[String]
) extends SlackEvent

case class GroupLeft (
  channel: String
) extends SlackEvent

case class GroupOpen (
  user: String,
  channel: String
) extends SlackEvent

case class GroupClose (
  user: String,
  channel: String
) extends SlackEvent

case class GroupArchive (
  channel: String
) extends SlackEvent

case class GroupUnarchive (
  channel: String
) extends SlackEvent

case class GroupRename (
  channel: Channel
) extends SlackEvent

case class GroupMarked (
  channel: String,
  ts: String
) extends SlackEvent

case class GroupHistoryChanged (
  latest: Long,
  ts: String,
  event_ts: String
) extends SlackEvent

case class FileCreated (
  file_id: String
) extends SlackEvent

case class FileShared (
  file_id: String
) extends SlackEvent

case class FileUnshared (
  file_id: String
) extends SlackEvent

case class FilePublic (
  file_id: String
) extends SlackEvent

case class FilePrivate (
  file: String
) extends SlackEvent

case class FileChange (
  file_id: String
) extends SlackEvent

case class FileDeleted (
  file_id: String,
  event_ts: String
) extends SlackEvent

case class FileCommentAdded (
  file_id: String,
  comment: JsValue // TODO: SlackComment?
) extends SlackEvent

case class FileCommentEdited (
  file_id: String,
  comment: JsValue // TODO: SlackComment?
) extends SlackEvent

case class FileCommentDeleted (
  file_id: String,
  comment: String
) extends SlackEvent


// Format of event is tbd
case class PinAdded (
  `type`: String
) extends SlackEvent

// Format of event is tbd
case class PinRemoved (
  `type`: String
) extends SlackEvent

case class PresenceChange (
  user: String,
  presence: String
) extends SlackEvent

case class ManualPresenceChange (
  presence: String
) extends SlackEvent

case class PrefChange (
  name: String,
  value: JsValue
) extends SlackEvent

case class UserChange (
  user: User
) extends SlackEvent

case class TeamJoin (
  user: User
) extends SlackEvent

case class StarAdded (
  user: String,
  item: JsValue, // TODO: Different item types -- https://api.slack.com/methods/stars.list
  event_ts: String
) extends SlackEvent

case class StarRemoved (
  user: String,
  item: JsValue, // TODO: Different item types -- https://api.slack.com/methods/stars.list
  event_ts: String
) extends SlackEvent

case class EmojiChanged (
  event_ts: String
) extends SlackEvent

case class CommandsChanged (
  event_ts: String
) extends SlackEvent

case class TeamPlanChanged (
  plan: String
) extends SlackEvent

case class TeamPrefChanged (
  name: String,
  value: String // TODO: Primitive type?
) extends SlackEvent

case class TeamRename (
  name: String
) extends SlackEvent

case class TeamDomainChange (
  url: String,
  domain: String
) extends SlackEvent

case class BotAdded (
  bot: JsValue // TODO: structure -- https://api.slack.com/events/bot_added
) extends SlackEvent

case class BotChanged (
  bot: JsValue // TODO: structure -- https://api.slack.com/events/bot_added
) extends SlackEvent

case class AccountsChanged (
  `type`: String
) extends SlackEvent

case class TeamMigrationStarted (
  `type`: String
) extends SlackEvent

case class ReconnectUrl (
  `type`: String,
  url: Option[String] // Optional because currently undocumented and could change
) extends SlackEvent

case class Reply(
  ok: Boolean,
  reply_to: Long,
  ts: String,
  text: String
) extends SlackEvent

case class AppsChanged(
  app: App,
  event_ts: String
) extends SlackEvent

case class AppsUninstalled(
  app_id: String,
  event_ts: String
) extends SlackEvent

case class AppsInstalled(
  app: App,
  event_ts: String
) extends SlackEvent

case class DesktopNotification(
  `type`: String,
  title: String,
  subtitle: String,
  msg: String,
  content: String,
  channel: String,
  launchUri: String,
  avatarImage: String,
  ssbFilename: String,
  imageUrl: Option[String],
  is_shared: Boolean,
  event_ts: String
) extends SlackEvent

case class DndUpdatedUser(
 `type`: String,
 user: String,
 dnd_status: DndStatus,
 event_ts: String
) extends SlackEvent

case class DndStatus(dnd_enabled: Boolean,
 next_dnd_start_ts: Long,
 next_dnd_end_ts: Long
)

case class MemberJoined(user: String,
                        channel: String,
                        inviter: String
                       ) extends SlackEvent
