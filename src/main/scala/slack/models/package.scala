package slack

import play.api.libs.json._

package object models {
  implicit val confirmFieldFmt = Json.format[ConfirmField]
  implicit val actionFieldFmt = Json.format[ActionField]
  implicit val attachmentFieldFmt = Json.format[AttachmentField]
  implicit val attachmentFmt = Json.format[Attachment]
  implicit val authIdentityFmt = Json.format[AuthIdentity]
  implicit val teamFmt = Json.format[Team]
  implicit val channelValueFmt = Json.format[ChannelValue]
  implicit val groupValueFmt = Json.format[GroupValue]
  implicit val imFmt = Json.format[Im]
  implicit val channelFmt = Json.format[Channel]
  implicit val groupFmt = Json.format[Group]
  implicit val userProfileFmt = Json.format[UserProfile]
  implicit val userFmt = Json.format[User]
  implicit val reactionFmt = Json.format[Reaction]
  implicit val slackCommentFmt = Json.format[SlackComment]
  implicit val slackFileFmt = Json.format[SlackFile]
  implicit val slackFileIdFmt = Json.format[SlackFileId]
  implicit val updateResponseFmt = Json.format[UpdateResponse]
  implicit val appFmt = Json.format[App]
  implicit val reactionMsgFmt = Json.format[ReactionItemMessage]
  implicit val reactionFileFmt = Json.format[ReactionItemFile]
  implicit val reactionFileCommentFmt = Json.format[ReactionItemFileComment]
  implicit val reactionItemReads = new Reads[ReactionItem] {
    def reads(json: JsValue): JsResult[ReactionItem] = {
      val rType = (json \ "type").asOpt[String]
      if (rType.isDefined) {
        rType.get match {
          case "message" => JsSuccess(json.as[ReactionItemMessage])
          case "file" => JsSuccess(json.as[ReactionItemFile])
          case "file_comment" => JsSuccess(json.as[ReactionItemFileComment])
          case t: String => JsError(JsonValidationError("Invalid type property: {}", t))
        }
      } else {
        JsError(JsonValidationError("Required (string) event type property is missing."))
      }
    }
  }
  implicit val reactionItemWrites = new Writes[ReactionItem] {
    override def writes(item: ReactionItem): JsValue = item match {
      case i:ReactionItemMessage => Json.toJson(i)
      case i:ReactionItemFile => Json.toJson(i)
      case i:ReactionItemFileComment => Json.toJson(i)
    }
  }
  implicit val optionElementFmt = Json.format[OptionElement]
  implicit val selectElementFmt = Json.format[SelectElement]
  implicit val textElementFmt = Json.format[TextElement]
  implicit val dialogElementReads = new Reads[DialogElement] {
    def reads(json: JsValue): JsResult[DialogElement] = {
      val rType = (json \ "type").asOpt[String]
      if (rType.isDefined) {
        rType.get match {
          case "select" => JsSuccess(json.as[SelectElement])
          case _ => JsSuccess(json.as[TextElement])
        }
      } else {
        JsError(JsonValidationError("Required property: [type] is missing."))
      }
    }
  }
  implicit val dialogElementWrites = new Writes[DialogElement] {
    override def writes(element: DialogElement): JsValue = element match {
      case e:TextElement => Json.toJson(e)
      case e:SelectElement => Json.toJson(e)
    }
  }
  implicit val dialogFmt = Json.format[Dialog]

  // Event Formats
  implicit val helloFmt = Json.format[Hello]
  implicit val messageFmt = Json.format[Message]
  implicit val messageReply = Json.format[Reply]
  implicit val editMessageFmt = Json.format[EditMessage]
  implicit val botMessageFmt = Json.format[BotMessage]
  implicit val messageChangedFmt = Json.format[MessageChanged]
  implicit val messageDeletedFmt = Json.format[MessageDeleted]
  implicit val reactionAddedFmt = Json.format[ReactionAdded]
  implicit val reactionRemovedFmt = Json.format[ReactionRemoved]
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
  implicit val mpImOpenFmt = Json.format[MpImOpen]
  implicit val mpImCloseFmt = Json.format[MpImClose]
  implicit val mpImJoinFmt = Json.format[MpImJoined]
  implicit val groupJoinFmt = Json.format[GroupJoined]
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
  implicit val reconnectUrlFmt = Json.format[ReconnectUrl]
  implicit val appsChangedFmt = Json.format[AppsChanged]
  implicit val appsUninstalledFmt = Json.format[AppsUninstalled]
  implicit val appsInstalledFmt = Json.format[AppsInstalled]
  implicit val desktopNotificationFmt = Json.format[DesktopNotification]
  implicit val dndStatusFmt = Json.format[DndStatus]
  implicit val dndUpdateUserFmt = Json.format[DndUpdatedUser]
  implicit val memberJoined = Json.format[MemberJoined]

  // Message sub-types
  import MessageSubtypes._

  implicit val messageSubtypeMeMessageFmt = Json.format[MeMessage]
  implicit val messageSubtypeChannelNameMessageFmt = Json.format[ChannelNameMessage]
  implicit val messageSubtypeFileShareMessageFmt = Json.format[FileShareMessage]
  implicit val messageSubtypeHandledSubtypeFmt = Json.format[UnhandledSubtype]
  implicit val messageWithSubtypeWrites: Writes[MessageWithSubtype] = {
    import play.api.libs.functional.syntax._
    (
      (JsPath \ "ts").write[String] and
        (JsPath \ "channel").write[String] and
        (JsPath \ "user").write[String] and
        (JsPath \ "text").write[String] and
        (JsPath \ "is_starred").write[Option[Boolean]] and
        (JsPath \ "subtype").write[String]
      ) ((msg: MessageWithSubtype) => (msg.ts, msg.channel, msg.user, msg.text, msg.is_starred, msg.messageSubType.subtype))
  }

  // Event Reads/Writes
  implicit val slackEventWrites = new Writes[SlackEvent] {
    def writes(event: SlackEvent) = {
      event match {
        case e: Hello => Json.toJson(e)
        case e: Message => Json.toJson(e)
        case e: Reply => Json.toJson(e)
        case e: MessageChanged => Json.toJson(e)
        case e: MessageDeleted => Json.toJson(e)
        case e: BotMessage => Json.toJson(e)
        case e: MessageWithSubtype => Json.toJson(e)
        case e: MeMessage => Json.toJson(e)
        case e: ChannelNameMessage => Json.toJson(e)
        case e: FileShareMessage => Json.toJson(e)
        case e: UnhandledSubtype => Json.toJson(e)
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
        case e: ReconnectUrl => Json.toJson(e)
        case e: AppsChanged => Json.toJson(e)
        case e: AppsUninstalled => Json.toJson(e)
        case e: AppsInstalled => Json.toJson(e)
        case e: DesktopNotification => Json.toJson(e)
        case e: DndUpdatedUser => Json.toJson(e)
        case e: MemberJoined => Json.toJson(e)
      }
    }
  }

  implicit val subMessageReads = new Reads[MessageWithSubtype] {
    def reads(jsValue: JsValue): JsResult[MessageWithSubtype] = {
      (jsValue \ "subtype").asOpt[String] match {
        case Some(subtype) =>
          import MessageSubtypes._
          val subMessage = subtype match {
            case "me_message" => jsValue.as[MeMessage]
            case "channel_name" => jsValue.as[ChannelNameMessage]
            case "file_share" => jsValue.as[FileShareMessage]
            case _ => jsValue.as[UnhandledSubtype]
          }
          JsSuccess(
            MessageWithSubtype(
              (jsValue \ "ts").as[String],
              (jsValue \ "channel").as[String],
              (jsValue \ "user").as[String],
              (jsValue \ "text").as[String],
              (jsValue \ "is_starred").asOpt[Boolean],
              subMessage
            )
          )
        case None => JsError("Not a message with a subtype.")
      }
    }
  }

  implicit val slackEventReads = new Reads[SlackEvent] {
    def reads(jsValue: JsValue): JsResult[SlackEvent] = {
      val etype = (jsValue \ "type").asOpt[String]
      val subtype = (jsValue \ "subtype").asOpt[String]
      if (etype.isDefined) {
        etype.get match {
          case "hello" => JsSuccess(jsValue.as[Hello])
          case "message" if subtype.contains("message_changed") => JsSuccess(jsValue.as[MessageChanged])
          case "message" if subtype.contains("message_deleted") => JsSuccess(jsValue.as[MessageDeleted])
          case "message" if subtype.contains("bot_message") => JsSuccess(jsValue.as[BotMessage])
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
          case "reconnect_url" => JsSuccess(jsValue.as[ReconnectUrl])
          case "apps_changed" => JsSuccess(jsValue.as[AppsChanged])
          case "apps_uninstalled" => JsSuccess(jsValue.as[AppsUninstalled])
          case "apps_installed" => JsSuccess(jsValue.as[AppsInstalled])
          case "desktop_notification" => JsSuccess(jsValue.as[DesktopNotification])
          case "dnd_updated_user" => JsSuccess(jsValue.as[DndUpdatedUser])
          case "member_joined_channel" => JsSuccess(jsValue.as[MemberJoined])
          case t: String => JsError(JsonValidationError("Invalid type property: {}", t))
        }
      } else if ((jsValue \ "reply_to").asOpt[Long].isDefined) {
        JsSuccess(jsValue.as[Reply])
      } else {
        JsError(JsonValidationError("Required (string) event type property is missing."))
      }
    }
  }
}
