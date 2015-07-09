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
  is_starred: Option[Boolean]
) extends SlackEvent

case class SubMessage (
  ts: String,
  user: String,
  text: String,
  is_starred: Option[Boolean]
) extends SlackEvent

// TODO: Message Sub-types
case class MessageWithSubtype (
  ts: String,
  message: SubMessage,
  subtype: String,
  hidden: Option[Boolean],
  event_ts: String,
  channel: String
) extends SlackEvent

case class ReactionAdded (
  reaction: String,
  item: JsValue, // TODO: Different item types -- https://api.slack.com/methods/stars.list
  event_ts: String,
  user: String
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

case class GroupJoin (
  channel: Channel
) extends SlackEvent

case class GroupLeft (
  channel: Channel
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
  file: SlackFile
) extends SlackEvent

case class FileShared (
  file: SlackFile
) extends SlackEvent

case class FileUnshared (
  file: SlackFile
) extends SlackEvent

case class FilePublic (
  file: SlackFile
) extends SlackEvent

case class FilePrivate (
  file: String
) extends SlackEvent

case class FileChange (
  file: SlackFile
) extends SlackEvent

case class FileDeleted (
  file_id: String,
  event_ts: String
) extends SlackEvent

case class FileCommentAdded (
  file: SlackFile,
  comment: JsValue // TODO: SlackComment?
) extends SlackEvent

case class FileCommentEdited (
  file: SlackFile,
  comment: JsValue // TODO: SlackComment?
) extends SlackEvent

case class FileCommentDeleted (
  file: SlackFile,
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
  value: String
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