package slack

import play.api.data.validation.ValidationError
import play.api.libs.json._

package object models {
  implicit val authIdentityFmt = Json.format[AuthIdentity]
  implicit val teamFmt = Json.format[Team]
  implicit val channelValueFmt = Json.format[ChannelValue]
  implicit val groupValueFmt = Json.format[GroupValue]
  implicit val imFmt = Json.format[Im]
  implicit val channelFmt = Json.format[Channel]
  implicit val groupFmt = Json.format[Group]
  implicit val userProfileFmt = Json.format[UserProfile]
  implicit val userFmt = Json.format[User]
  implicit val slackCommentFmt = Json.format[SlackComment]
  implicit val slackFileFmt = Json.format[SlackFile]

  // Event Formats
  implicit val helloFmt = Json.format[Hello]
  implicit val messageFmt = Json.format[Message]
  implicit val subMessageFmt= Json.format[SubMessage]
  implicit val messageWithSubtypeFmt = Json.format[MessageWithSubtype]
  implicit val reactionAddedFmt= Json.format[ReactionAdded]
  implicit val reactionRemovedFmt= Json.format[ReactionRemoved]
  implicit val userTypingFmt = Json.format[UserTyping]
  implicit val channelMarkedFmt = Json.format[ChannelMarked]
  implicit val channelCreatedFmt = Json.format[ChannelCreated]
  implicit val channelJoinedFmt = Json.format[ChannelJoined]
  implicit val channelLeftFmt = Json.format[ChannelLeft]
  implicit val channelDeletedFmt = Json.format[ChannelDeleted]
  implicit val channelRenameFmt = Json.format[ChannelRename]
  implicit val channelArchiveFmt = Json.format[ChannelArchive]
  implicit val channelUnarchiveFmt = Json.format[ChannelUnarchive]
  implicit val channelHistoryChangedFmt = Json.format[ChannelHistoryChanged]
  implicit val imCreatedFmt = Json.format[ImCreated]
  implicit val imOpenedFmt = Json.format[ImOpened]
  implicit val imCloseFmt = Json.format[ImClose]
  implicit val imMarkedFmt = Json.format[ImMarked]
  implicit val imHistoryChangedFmt = Json.format[ImHistoryChanged]
  implicit val groupJoinFmt = Json.format[GroupJoin]
  implicit val groupLeftFmt = Json.format[GroupLeft]
  implicit val groupOpenFmt = Json.format[GroupOpen]
  implicit val groupCloseFmt = Json.format[GroupClose]
  implicit val groupArchiveFmt = Json.format[GroupArchive]
  implicit val groupUnarchiveFmt = Json.format[GroupUnarchive]
  implicit val groupRenameFmt = Json.format[GroupRename]
  implicit val groupMarkedFmt = Json.format[GroupMarked]
  implicit val groupHistoryChangedFmt = Json.format[GroupHistoryChanged]
  implicit val fileCreatedFmt = Json.format[FileCreated]
  implicit val fileSharedFmt = Json.format[FileShared]
  implicit val fileUnsharedFmt = Json.format[FileUnshared]
  implicit val filePublicFmt = Json.format[FilePublic]
  implicit val filePrivateFmt = Json.format[FilePrivate]
  implicit val fileChangeFmt = Json.format[FileChange]
  implicit val fileDeletedFmt = Json.format[FileDeleted]
  implicit val fileCommentAddedFmt = Json.format[FileCommentAdded]
  implicit val fileCommentEditedFmt = Json.format[FileCommentEdited]
  implicit val fileCommentDeletedFmt = Json.format[FileCommentDeleted]
  implicit val pinAddedFmt = Json.format[PinAdded]
  implicit val pinRemovedFmt = Json.format[PinRemoved]
  implicit val presenceChangeFmt = Json.format[PresenceChange]
  implicit val manualPresenceChangeFmt = Json.format[ManualPresenceChange]
  implicit val prefChangeFmt = Json.format[PrefChange]
  implicit val userChangeFmt = Json.format[UserChange]
  implicit val teamJoinFmt = Json.format[TeamJoin]
  implicit val starAddedFmt = Json.format[StarAdded]
  implicit val starRemovedFmt = Json.format[StarRemoved]
  implicit val emojiChangedFmt = Json.format[EmojiChanged]
  implicit val commandsChangedFmt = Json.format[CommandsChanged]
  implicit val teamPlanChangedFmt = Json.format[TeamPlanChanged]
  implicit val teamPrefChangedFmt = Json.format[TeamPrefChanged]
  implicit val teamRenameFmt = Json.format[TeamRename]
  implicit val teamDomainChangeFmt = Json.format[TeamDomainChange]
  implicit val botAddedFmt = Json.format[BotAdded]
  implicit val botChangedFmt = Json.format[BotChanged]
  implicit val accountsChangedFmt = Json.format[AccountsChanged]
  implicit val teamMigrationStartedFmt = Json.format[TeamMigrationStarted]

  // Event Reads/Writes
  implicit val slackEventWrites = new Writes[SlackEvent] {
    def writes(event: SlackEvent) = {
      event match {
        case e: Hello => Json.toJson(e)
        case e: Message => Json.toJson(e)
        case e: MessageWithSubtype => Json.toJson(e)
        case e: SubMessage => Json.toJson(e)
        case e: UserTyping => Json.toJson(e)
        case e: ReactionAdded => Json.toJson(e)
        case e: ReactionRemoved => Json.toJson(e)
        case e: ChannelMarked => Json.toJson(e)
        case e: ChannelCreated => Json.toJson(e)
        case e: ChannelJoined => Json.toJson(e)
        case e: ChannelLeft => Json.toJson(e)
        case e: ChannelDeleted => Json.toJson(e)
        case e: ChannelRename => Json.toJson(e)
        case e: ChannelArchive => Json.toJson(e)
        case e: ChannelUnarchive => Json.toJson(e)
        case e: ChannelHistoryChanged => Json.toJson(e)
        case e: ImCreated => Json.toJson(e)
        case e: ImOpened => Json.toJson(e)
        case e: ImClose => Json.toJson(e)
        case e: ImMarked => Json.toJson(e)
        case e: ImHistoryChanged => Json.toJson(e)
        case e: GroupJoin => Json.toJson(e)
        case e: GroupLeft => Json.toJson(e)
        case e: GroupOpen => Json.toJson(e)
        case e: GroupClose => Json.toJson(e)
        case e: GroupArchive => Json.toJson(e)
        case e: GroupUnarchive => Json.toJson(e)
        case e: GroupRename => Json.toJson(e)
        case e: GroupMarked => Json.toJson(e)
        case e: GroupHistoryChanged => Json.toJson(e)
        case e: FileCreated => Json.toJson(e)
        case e: FileShared => Json.toJson(e)
        case e: FileUnshared => Json.toJson(e)
        case e: FilePublic => Json.toJson(e)
        case e: FilePrivate => Json.toJson(e)
        case e: FileChange => Json.toJson(e)
        case e: FileDeleted => Json.toJson(e)
        case e: FileCommentAdded => Json.toJson(e)
        case e: FileCommentEdited => Json.toJson(e)
        case e: FileCommentDeleted => Json.toJson(e)
        case e: PinAdded => Json.toJson(e)
        case e: PinRemoved => Json.toJson(e)
        case e: PresenceChange => Json.toJson(e)
        case e: ManualPresenceChange => Json.toJson(e)
        case e: PrefChange => Json.toJson(e)
        case e: UserChange => Json.toJson(e)
        case e: TeamJoin => Json.toJson(e)
        case e: StarAdded => Json.toJson(e)
        case e: StarRemoved => Json.toJson(e)
        case e: EmojiChanged => Json.toJson(e)
        case e: CommandsChanged => Json.toJson(e)
        case e: TeamPlanChanged => Json.toJson(e)
        case e: TeamPrefChanged => Json.toJson(e)
        case e: TeamRename => Json.toJson(e)
        case e: TeamDomainChange => Json.toJson(e)
        case e: BotAdded => Json.toJson(e)
        case e: BotChanged => Json.toJson(e)
        case e: AccountsChanged => Json.toJson(e)
        case e: TeamMigrationStarted => Json.toJson(e)
      }
    }
  }

  implicit val slackEventReads = new Reads[SlackEvent] {
    def reads(jsValue: JsValue): JsResult[SlackEvent] = {
      val etype = (jsValue \ "type").asOpt[String]
      val subtype = (jsValue \ "subtype").asOpt[String]
      if(etype.isDefined) {
        etype.get match {
          case "hello" => JsSuccess(jsValue.as[Hello])
          case "message" if subtype.isDefined => JsSuccess(jsValue.as[MessageWithSubtype])
          case "message" => JsSuccess(jsValue.as[Message])
          case "user_typing" => JsSuccess(jsValue.as[UserTyping])
          case "reaction_added" => JsSuccess(jsValue.as[ReactionAdded])
          case "reaction_removed" => JsSuccess(jsValue.as[ReactionRemoved])
          case "channel_marked" => JsSuccess(jsValue.as[ChannelMarked])
          case "channel_created" => JsSuccess(jsValue.as[ChannelCreated])
          case "channel_joined" => JsSuccess(jsValue.as[ChannelJoined])
          case "channel_left" => JsSuccess(jsValue.as[ChannelLeft])
          case "channel_deleted" => JsSuccess(jsValue.as[ChannelDeleted])
          case "channel_rename" => JsSuccess(jsValue.as[ChannelRename])
          case "channel_archive" => JsSuccess(jsValue.as[ChannelArchive])
          case "channel_unarchive" => JsSuccess(jsValue.as[ChannelUnarchive])
          case "channel_history_changed" => JsSuccess(jsValue.as[ChannelHistoryChanged])
          case "im_created" => JsSuccess(jsValue.as[ImCreated])
          case "im_open" => JsSuccess(jsValue.as[ImOpened])
          case "im_close" => JsSuccess(jsValue.as[ImClose])
          case "im_marked" => JsSuccess(jsValue.as[ImMarked])
          case "im_history_changed" => JsSuccess(jsValue.as[ImHistoryChanged])
          case "group_join" => JsSuccess(jsValue.as[GroupJoin])
          case "group_left" => JsSuccess(jsValue.as[GroupLeft])
          case "group_open" => JsSuccess(jsValue.as[GroupOpen])
          case "group_close" => JsSuccess(jsValue.as[GroupClose])
          case "group_archive" => JsSuccess(jsValue.as[GroupArchive])
          case "group_unarchive" => JsSuccess(jsValue.as[GroupUnarchive])
          case "group_rename" => JsSuccess(jsValue.as[GroupRename])
          case "group_marked" => JsSuccess(jsValue.as[GroupMarked])
          case "grouo_history_changed" => JsSuccess(jsValue.as[GroupHistoryChanged])
          case "file_created" => JsSuccess(jsValue.as[FileCreated])
          case "file_shared" => JsSuccess(jsValue.as[FileShared])
          case "file_unshared" => JsSuccess(jsValue.as[FileUnshared])
          case "file_public" => JsSuccess(jsValue.as[FilePublic])
          case "file_private" => JsSuccess(jsValue.as[FilePrivate])
          case "file_change" => JsSuccess(jsValue.as[FileChange])
          case "file_deleted" => JsSuccess(jsValue.as[FileDeleted])
          case "file_comment_added" => JsSuccess(jsValue.as[FileCommentAdded])
          case "file_comment_edited" => JsSuccess(jsValue.as[FileCommentEdited])
          case "file_comment_deleted" => JsSuccess(jsValue.as[FileCommentDeleted])
          case "pin_added" => JsSuccess(jsValue.as[PinAdded])
          case "pin_removed" => JsSuccess(jsValue.as[PinRemoved])
          case "presence_change" => JsSuccess(jsValue.as[PresenceChange])
          case "manual_presence_change" => JsSuccess(jsValue.as[ManualPresenceChange])
          case "pref_change" => JsSuccess(jsValue.as[PrefChange])
          case "user_change" => JsSuccess(jsValue.as[UserChange])
          case "team_join" => JsSuccess(jsValue.as[TeamJoin])
          case "star_added" => JsSuccess(jsValue.as[StarAdded])
          case "star_removed" => JsSuccess(jsValue.as[StarRemoved])
          case "emoji_changed" => JsSuccess(jsValue.as[EmojiChanged])
          case "commands_changed" => JsSuccess(jsValue.as[CommandsChanged])
          case "team_plan_changed" => JsSuccess(jsValue.as[TeamPlanChanged])
          case "team_pref_changed" => JsSuccess(jsValue.as[TeamPrefChanged])
          case "team_rename" => JsSuccess(jsValue.as[TeamRename])
          case "team_domain_change" => JsSuccess(jsValue.as[TeamDomainChange])
          case "bot_added" => JsSuccess(jsValue.as[BotAdded])
          case "bot_changed" => JsSuccess(jsValue.as[BotChanged])
          case "accounts_changed" => JsSuccess(jsValue.as[AccountsChanged])
          case "team_migration_started" => JsSuccess(jsValue.as[TeamMigrationStarted])
          case t: String => JsError(ValidationError("Invalid type property: {}", t))
        }
      } else {
        JsError(ValidationError("Required (string) event type property is missing."))
      }
    }
  }
}