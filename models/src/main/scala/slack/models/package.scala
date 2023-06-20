package slack

import play.api.libs.json._

import scala.util.{Failure, Success, Try}

package object models {

  def eitherObjectFormat[A, B](leftKey: String, rightKey: String)(implicit aFormat: Format[A], bFormat: Format[B]): Format[Either[A, B]] =
    Format(new Reads[Either[A, B]] {
      override def reads(json: JsValue): JsResult[Either[A, B]] = {
        Try {
          val left = (json \ leftKey).asOpt[String]
          left match {
            case Some(_) => Left(json.as[A])
            case None => Right(json.as[B])
          }
        } match {
          case Success(e) => JsSuccess(e)
          case Failure(e) => JsError(e.getMessage)
      }
      }
    }, new Writes[Either[A, B]] {
      override def writes(o: Either[A, B]): JsValue =
        o match {
          case Left(a) => Json.toJson(a)
          case Right(b) => Json.toJson(b)
        }
    })

  implicit val confirmFieldFmt: Format[ConfirmField] = Json.format[ConfirmField]
  implicit val actionFieldFmt: Format[ActionField] = Json.format[ActionField]
  implicit val attachmentFieldFmt: Format[AttachmentField] = Json.format[AttachmentField]
  implicit val attachmentFmt: Format[Attachment] = Json.format[Attachment]
  implicit val authIdentityFmt: Format[AuthIdentity] = Json.format[AuthIdentity]
  implicit val teamFmt: Format[Team] = Json.format[Team]
  implicit val subteamFmt: Format[Subteam] = Json.format[Subteam]
  implicit val channelValueFmt: Format[ChannelValue] = Json.format[ChannelValue]
  implicit val groupValueFmt: Format[GroupValue] = Json.format[GroupValue]
  implicit val imFmt: Format[Im] = Json.format[Im]
  implicit val channelFmt: Format[Channel] = Json.format[Channel]
  implicit val groupFmt: Format[Group] = Json.format[Group]
  implicit val userProfileFmt: Format[UserProfile] = Json.format[UserProfile]
  implicit val userFmt: Format[User] = Json.format[User]
  implicit val reactionFmt: Format[Reaction] = Json.format[Reaction]
  implicit val slackCommentFmt: Format[SlackComment] = Json.format[SlackComment]
  implicit val slackFileFmt: Format[SlackFile] = Json.format[SlackFile]
  implicit val slackFileIdFmt: Format[SlackFileId] = Json.format[SlackFileId]
  implicit val updateResponseFmt: Format[UpdateResponse] = Json.format[UpdateResponse]
  implicit val appFmt: Format[App] = Json.format[App]
  implicit val reactionMsgFmt: Format[ReactionItemMessage] = Json.format[ReactionItemMessage]
  implicit val reactionFileFmt: Format[ReactionItemFile] = Json.format[ReactionItemFile]
  implicit val reactionFileCommentFmt: Format[ReactionItemFileComment] = Json.format[ReactionItemFileComment]
  implicit val reactionItemReads: Reads[ReactionItem] = new Reads[ReactionItem] {
    def reads(json: JsValue): JsResult[ReactionItem] = {
     (json \ "type").asOpt[String] match {
        case Some("message") => JsSuccess(json.as[ReactionItemMessage])
        case Some("file") => JsSuccess(json.as[ReactionItemFile])
        case Some("file_comment") => JsSuccess(json.as[ReactionItemFileComment])
        case Some(t: String) => JsError(JsonValidationError("Invalid type property: {}", t))
        case None => JsError(JsonValidationError("Required (string) event type property is missing."))
      }
    }
  }
  implicit val reactionItemWrites: Writes[ReactionItem] = new Writes[ReactionItem] {
    override def writes(item: ReactionItem): JsValue = item match {
      case i: ReactionItemMessage => Json.toJson(i)
      case i: ReactionItemFile => Json.toJson(i)
      case i: ReactionItemFileComment => Json.toJson(i)
    }
  }
  implicit val optionElementFmt: Format[OptionElement] = Json.format[OptionElement]
  implicit val selectElementFmt: Format[SelectElement] = Json.format[SelectElement]
  implicit val textElementFmt: Format[TextElement] = Json.format[TextElement]
  implicit val dialogElementReads: Reads[DialogElement] = new Reads[DialogElement] {
    def reads(json: JsValue): JsResult[DialogElement] = {
      (json \ "type").asOpt[String] match {
        case Some("select") => JsSuccess(json.as[SelectElement])
        case Some(_) => JsSuccess(json.as[TextElement])
        case None => JsError(JsonValidationError("Required property: [type] is missing."))
      }
    }
  }
  implicit val dialogElementWrites: Writes[DialogElement] = new Writes[DialogElement] {
    override def writes(element: DialogElement): JsValue = element match {
      case e: TextElement => Json.toJson(e)
      case e: SelectElement => Json.toJson(e)
    }
  }
  implicit val dialogFmt: Format[Dialog] = Json.format[Dialog]

  // Event Formats
  implicit val helloFmt: Format[Hello] = Json.format[Hello]
  implicit val messageFmt: Format[Message] = Json.format[Message]
  implicit val messageReply: Format[Reply] = Json.format[Reply]
  implicit val replyMarkerFmt: Format[ReplyMarker] = Json.format[ReplyMarker]
  implicit val editMessageFmt: Format[EditMessage] = Json.format[EditMessage]
  implicit val replyMessageFmt: Format[ReplyMessage] = Json.format[ReplyMessage]
  implicit val replyBotMessageFmt: Format[ReplyBotMessage] = Json.format[ReplyBotMessage]
  implicit val messageChangedFmt: Format[MessageChanged] = Json.format[MessageChanged]
  implicit val messageDeletedFmt: Format[MessageDeleted] = Json.format[MessageDeleted]
  implicit val messageRepliedFmt: Format[MessageReplied] = Json.format[MessageReplied]
  implicit val botMessageRepliedFmt: Format[BotMessageReplied] = Json.format[BotMessageReplied]
  implicit val reactionAddedFmt: Format[ReactionAdded] = Json.format[ReactionAdded]
  implicit val reactionRemovedFmt: Format[ReactionRemoved] = Json.format[ReactionRemoved]
  implicit val userTypingFmt: Format[UserTyping] = Json.format[UserTyping]
  implicit val channelMarkedFmt: Format[ChannelMarked] = Json.format[ChannelMarked]
  implicit val channelCreatedFmt: Format[ChannelCreated] = Json.format[ChannelCreated]
  implicit val channelJoinedFmt: Format[ChannelJoined] = Json.format[ChannelJoined]
  implicit val channelLeftFmt: Format[ChannelLeft] = Json.format[ChannelLeft]
  implicit val channelDeletedFmt: Format[ChannelDeleted] = Json.format[ChannelDeleted]
  implicit val channelRenameFmt: Format[ChannelRename] = Json.format[ChannelRename]
  implicit val channelArchiveFmt: Format[ChannelArchive] = Json.format[ChannelArchive]
  implicit val channelUnarchiveFmt: Format[ChannelUnarchive] = Json.format[ChannelUnarchive]
  implicit val channelHistoryChangedFmt: Format[ChannelHistoryChanged] = Json.format[ChannelHistoryChanged]
  implicit val channelTopicChangedFmt: Format[ChannelTopicChanged] = Json.format[ChannelTopicChanged]
  implicit val imCreatedFmt: Format[ImCreated] = Json.format[ImCreated]
  implicit val imOpenedFmt: Format[ImOpened] = Json.format[ImOpened]
  implicit val imCloseFmt: Format[ImClose] = Json.format[ImClose]
  implicit val imMarkedFmt: Format[ImMarked] = Json.format[ImMarked]
  implicit val imHistoryChangedFmt: Format[ImHistoryChanged] = Json.format[ImHistoryChanged]
  implicit val mpImOpenFmt: Format[MpImOpen] = Json.format[MpImOpen]
  implicit val mpImCloseFmt: Format[MpImClose] = Json.format[MpImClose]
  implicit val mpImJoinFmt: Format[MpImJoined] = Json.format[MpImJoined]
  implicit val groupJoinFmt: Format[GroupJoined] = Json.format[GroupJoined]
  implicit val groupLeftFmt: Format[GroupLeft] = Json.format[GroupLeft]
  implicit val groupOpenFmt: Format[GroupOpen] = Json.format[GroupOpen]
  implicit val groupCloseFmt: Format[GroupClose] = Json.format[GroupClose]
  implicit val groupArchiveFmt: Format[GroupArchive] = Json.format[GroupArchive]
  implicit val groupUnarchiveFmt: Format[GroupUnarchive] = Json.format[GroupUnarchive]
  implicit val groupRenameFmt: Format[GroupRename] = Json.format[GroupRename]
  implicit val groupMarkedFmt: Format[GroupMarked] = Json.format[GroupMarked]
  implicit val groupHistoryChangedFmt: Format[GroupHistoryChanged] = Json.format[GroupHistoryChanged]
  implicit val fileCreatedFmt: Format[FileCreated] = Json.format[FileCreated]
  implicit val fileSharedFmt: Format[FileShared] = Json.format[FileShared]
  implicit val fileUnsharedFmt: Format[FileUnshared] = Json.format[FileUnshared]
  implicit val filePublicFmt: Format[FilePublic] = Json.format[FilePublic]
  implicit val filePrivateFmt: Format[FilePrivate] = Json.format[FilePrivate]
  implicit val fileChangeFmt: Format[FileChange] = Json.format[FileChange]
  implicit val fileDeletedFmt: Format[FileDeleted] = Json.format[FileDeleted]
  implicit val fileCommentAddedFmt: Format[FileCommentAdded] = Json.format[FileCommentAdded]
  implicit val fileCommentEditedFmt: Format[FileCommentEdited] = Json.format[FileCommentEdited]
  implicit val fileCommentDeletedFmt: Format[FileCommentDeleted] = Json.format[FileCommentDeleted]
  implicit val pinAddedFmt: Format[PinAdded] = Json.format[PinAdded]
  implicit val pinRemovedFmt: Format[PinRemoved] = Json.format[PinRemoved]
  implicit val presenceChangeFmt: Format[PresenceChange] = Json.format[PresenceChange]
  implicit val manualPresenceChangeFmt: Format[ManualPresenceChange] = Json.format[ManualPresenceChange]
  implicit val prefChangeFmt: Format[PrefChange] = Json.format[PrefChange]
  implicit val userChangeFmt: Format[UserChange] = Json.format[UserChange]
  implicit val userProfileChangeFmt: Format[UserProfileChanged] = Json.format[UserProfileChanged]
  implicit val userHuddleChangeFmt: Format[UserHuddleChanged] = Json.format[UserHuddleChanged]
  implicit val userStatusChangeFmt: Format[UserStatusChanged] = Json.format[UserStatusChanged]
  implicit val teamJoinFmt: Format[TeamJoin] = Json.format[TeamJoin]
  implicit val starAddedFmt: Format[StarAdded] = Json.format[StarAdded]
  implicit val starRemovedFmt: Format[StarRemoved] = Json.format[StarRemoved]
  implicit val emojiChangedFmt: Format[EmojiChanged] = Json.format[EmojiChanged]
  implicit val commandsChangedFmt: Format[CommandsChanged] = Json.format[CommandsChanged]
  implicit val teamPlanChangedFmt: Format[TeamPlanChanged] = Json.format[TeamPlanChanged]
  implicit val teamPrefChangedFmt: Format[TeamPrefChanged] = Json.format[TeamPrefChanged]
  implicit val teamRenameFmt: Format[TeamRename] = Json.format[TeamRename]
  implicit val teamDomainChangeFmt: Format[TeamDomainChange] = Json.format[TeamDomainChange]
  implicit val subteamCreatedFmt: Format[SubteamCreated] = Json.format[SubteamCreated]
  implicit val subteamUpdatedFmt: Format[SubteamUpdated] = Json.format[SubteamUpdated]
  implicit val subteamMembersChangedFmt: Format[SubteamMembersChanged] = Json.format[SubteamMembersChanged]
  implicit val botAddedFmt: Format[BotAdded] = Json.format[BotAdded]
  implicit val botChangedFmt: Format[BotChanged] = Json.format[BotChanged]
  implicit val accountsChangedFmt: Format[AccountsChanged] = Json.format[AccountsChanged]
  implicit val teamMigrationStartedFmt: Format[TeamMigrationStarted] = Json.format[TeamMigrationStarted]
  implicit val reconnectUrlFmt: Format[ReconnectUrl] = Json.format[ReconnectUrl]
  implicit val appsChangedFmt: Format[AppsChanged] = Json.format[AppsChanged]
  implicit val appActionsUpdatedFmt: Format[AppActionsUpdated] = Json.format[AppActionsUpdated]
  implicit val appsUninstalledFmt: Format[AppsUninstalled] = Json.format[AppsUninstalled]
  implicit val appsInstalledFmt: Format[AppsInstalled] = Json.format[AppsInstalled]
  implicit val desktopNotificationFmt: Format[DesktopNotification] = Json.format[DesktopNotification]
  implicit val dndStatusFmt: Format[DndStatus] = Json.format[DndStatus]
  implicit val dndUpdateUserFmt: Format[DndUpdatedUser] = Json.format[DndUpdatedUser]
  implicit val memberJoined: Format[MemberJoined] = Json.format[MemberJoined]
  implicit val memberLeft: Format[MemberLeft] = Json.format[MemberLeft]
  implicit val pong: Format[Pong] = Json.format[Pong]

  // Event Reads/Writes
  implicit val slackEventWrites: Writes[SlackEvent] = new Writes[SlackEvent] {
    def writes(event: SlackEvent) = {
      event match {
        case e: Hello => Json.toJson(e)
        case e: Message => Json.toJson(e)
        case e: Reply => Json.toJson(e)
        case e: MessageChanged => Json.toJson(e)
        case e: MessageDeleted => Json.toJson(e)
        case e: MessageReplied => Json.toJson(e)
        case e: BotMessageReplied => Json.toJson(e)
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
        case e: ChannelTopicChanged => Json.toJson(e)
        case e: ImCreated => Json.toJson(e)
        case e: ImOpened => Json.toJson(e)
        case e: ImClose => Json.toJson(e)
        case e: ImMarked => Json.toJson(e)
        case e: ImHistoryChanged => Json.toJson(e)
        case e: MpImOpen => Json.toJson(e)
        case e: MpImClose => Json.toJson(e)
        case e: MpImJoined => Json.toJson(e)
        case e: GroupJoined => Json.toJson(e)
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
        case e: UserProfileChanged => Json.toJson(e)
        case e: UserHuddleChanged => Json.toJson(e)
        case e: UserStatusChanged => Json.toJson(e)
        case e: TeamJoin => Json.toJson(e)
        case e: StarAdded => Json.toJson(e)
        case e: StarRemoved => Json.toJson(e)
        case e: EmojiChanged => Json.toJson(e)
        case e: CommandsChanged => Json.toJson(e)
        case e: TeamPlanChanged => Json.toJson(e)
        case e: TeamPrefChanged => Json.toJson(e)
        case e: TeamRename => Json.toJson(e)
        case e: TeamDomainChange => Json.toJson(e)
        case e: SubteamCreated => Json.toJson(e)
        case e: SubteamUpdated => Json.toJson(e)
        case e: SubteamMembersChanged => Json.toJson(e)
        case e: BotAdded => Json.toJson(e)
        case e: BotChanged => Json.toJson(e)
        case e: AccountsChanged => Json.toJson(e)
        case e: TeamMigrationStarted => Json.toJson(e)
        case e: ReconnectUrl => Json.toJson(e)
        case e: AppActionsUpdated => Json.toJson(e)
        case e: AppsChanged => Json.toJson(e)
        case e: AppsUninstalled => Json.toJson(e)
        case e: AppsInstalled => Json.toJson(e)
        case e: DesktopNotification => Json.toJson(e)
        case e: DndUpdatedUser => Json.toJson(e)
        case e: MemberJoined => Json.toJson(e)
        case e: MemberLeft => Json.toJson(e)
        case e: Pong => Json.toJson(e)
      }
    }
  }

  implicit val slackEventReads: Reads[SlackEvent] = new Reads[SlackEvent] {
    def reads(jsValue: JsValue): JsResult[SlackEvent] = {
      val etype = (jsValue \ "type").asOpt[String]
      val subtype = (jsValue \ "subtype").asOpt[String]
      val subMessageSubtype = (jsValue \ "message" \ "subtype").asOpt[String]
      if (etype.isDefined) {
        etype.get match {
          case "hello" => JsSuccess(jsValue.as[Hello])
          case "message" if subtype.contains("message_changed") => JsSuccess(jsValue.as[MessageChanged])
          case "message" if subtype.contains("message_deleted") => JsSuccess(jsValue.as[MessageDeleted])
          case "message" if subtype.contains("message_replied") && subMessageSubtype.contains("bot_message")
              => JsSuccess(jsValue.as[BotMessageReplied])
          case "message" if subtype.contains("message_replied") => JsSuccess(jsValue.as[MessageReplied])
          case "message" if subtype.contains("channel_topic") => JsSuccess(jsValue.as[ChannelTopicChanged])
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
          case "mpim_open" => JsSuccess(jsValue.as[MpImOpen])
          case "mpim_close" => JsSuccess(jsValue.as[MpImClose])
          case "mpim_joined" => JsSuccess(jsValue.as[MpImJoined])
          case "group_joined" => JsSuccess(jsValue.as[GroupJoined])
          case "group_left" => JsSuccess(jsValue.as[GroupLeft])
          case "group_open" => JsSuccess(jsValue.as[GroupOpen])
          case "group_close" => JsSuccess(jsValue.as[GroupClose])
          case "group_archive" => JsSuccess(jsValue.as[GroupArchive])
          case "group_unarchive" => JsSuccess(jsValue.as[GroupUnarchive])
          case "group_rename" => JsSuccess(jsValue.as[GroupRename])
          case "group_marked" => JsSuccess(jsValue.as[GroupMarked])
          case "group_history_changed" => JsSuccess(jsValue.as[GroupHistoryChanged])
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
          case "user_profile_changed" => JsSuccess(jsValue.as[UserProfileChanged])
          case "user_huddle_changed" => JsSuccess(jsValue.as[UserHuddleChanged])
          case "user_status_changed" => JsSuccess(jsValue.as[UserStatusChanged])
          case "team_join" => JsSuccess(jsValue.as[TeamJoin])
          case "star_added" => JsSuccess(jsValue.as[StarAdded])
          case "star_removed" => JsSuccess(jsValue.as[StarRemoved])
          case "emoji_changed" => JsSuccess(jsValue.as[EmojiChanged])
          case "commands_changed" => JsSuccess(jsValue.as[CommandsChanged])
          case "team_plan_changed" => JsSuccess(jsValue.as[TeamPlanChanged])
          case "team_pref_changed" => JsSuccess(jsValue.as[TeamPrefChanged])
          case "team_rename" => JsSuccess(jsValue.as[TeamRename])
          case "team_domain_change" => JsSuccess(jsValue.as[TeamDomainChange])
          case "subteam_created" => JsSuccess(jsValue.as[SubteamCreated])
          case "subteam_updated" => JsSuccess(jsValue.as[SubteamUpdated])
          case "subteam_members_changed" => JsSuccess(jsValue.as[SubteamMembersChanged])
          case "bot_added" => JsSuccess(jsValue.as[BotAdded])
          case "bot_changed" => JsSuccess(jsValue.as[BotChanged])
          case "accounts_changed" => JsSuccess(jsValue.as[AccountsChanged])
          case "team_migration_started" => JsSuccess(jsValue.as[TeamMigrationStarted])
          case "reconnect_url" => JsSuccess(jsValue.as[ReconnectUrl])
          case "apps_changed" => JsSuccess(jsValue.as[AppsChanged])
          case "app_actions_updated" => JsSuccess(jsValue.as[AppActionsUpdated])
          case "apps_uninstalled" => JsSuccess(jsValue.as[AppsUninstalled])
          case "apps_installed" => JsSuccess(jsValue.as[AppsInstalled])
          case "desktop_notification" => JsSuccess(jsValue.as[DesktopNotification])
          case "dnd_updated_user" => JsSuccess(jsValue.as[DndUpdatedUser])
          case "member_joined_channel" => JsSuccess(jsValue.as[MemberJoined])
          case "member_left_channel" => JsSuccess(jsValue.as[MemberLeft])
          case "pong" => JsSuccess(jsValue.as[Pong])
          case t: String => JsError(JsonValidationError("Invalid type property: {}", t))
        }
      } else if ((jsValue \ "reply_to").asOpt[Long].isDefined) {
        JsSuccess(jsValue.as[Reply])
      } else {
        JsError(JsonValidationError("Required (string) event type property is missing."))
      }
    }
  }
  implicit val slackEventStructureFmt: Format[SlackEventStructure] = Json.format[SlackEventStructure]
  implicit val eventServerChallengeFmt: Format[EventServerChallenge] = Json.format[EventServerChallenge]

  implicit val responseMetadataFormat: Format[ResponseMetadata] = Json.format[ResponseMetadata]
}
