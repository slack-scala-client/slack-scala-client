package slack.api

import java.io.File

import akka.actor.ActorSystem
import play.api.libs.json.JsValue
import slack.models.{Attachment, AuthIdentity, Block, Channel, Group, Im, Reaction, SlackFile, UpdateResponse, User}

trait SlackApiClientF[F[_]] {

  /**************************/
  /***   Test Endpoints   ***/
  /**************************/
  def test()(implicit system: ActorSystem): F[Boolean]
  def testAuth()(implicit system: ActorSystem): F[AuthIdentity]

  /***************************/
  /***  Channel Endpoints  ***/
  /***************************/
  def archiveChannel(channelId: String)(implicit system: ActorSystem): F[Boolean]
  def createChannel(name: String)(implicit system: ActorSystem): F[Channel]
  def getChannelHistory(channelId: String,
    latest: Option[String] = None,
    oldest: Option[String] = None,
    inclusive: Option[Int] = None,
    count: Option[Int] = None)(implicit system: ActorSystem): F[HistoryChunk]
  def getChannelInfo(channelId: String)(implicit system: ActorSystem): F[Channel]
  def inviteToChannel(channelId: String, userId: String)(implicit system: ActorSystem): F[Channel]
  def joinChannel(channelId: String)(implicit system: ActorSystem): F[Channel]
  def kickFromChannel(channelId: String, userId: String)(implicit system: ActorSystem): F[Boolean]
  def listChannels(excludeArchived: Boolean = false)(implicit system: ActorSystem): F[Seq[Channel]]
  def leaveChannel(channelId: String)(implicit system: ActorSystem): F[Boolean]
  def markChannel(channelId: String, ts: String)(implicit system: ActorSystem): F[Boolean]

  // TODO: Lite Channel Object
  def renameChannel(channelId: String, name: String)(implicit system: ActorSystem): F[Boolean]
  def getChannelReplies(channelId: String, thread_ts: String)(implicit system: ActorSystem): F[RepliesChunk]
  def setChannelPurpose(channelId: String, purpose: String)(implicit system: ActorSystem): F[String]
  def setChannelTopic(channelId: String, topic: String)(implicit system: ActorSystem): F[String]

  def unarchiveChannel(channelId: String)(implicit system: ActorSystem): F[Boolean]

  /**************************/
  /****  Chat Endpoints  ****/
  /**************************/
  def deleteChat(channelId: String, ts: String, asUser: Option[Boolean] = None)(implicit system: ActorSystem): F[Boolean]

  def postChatMessage(channelId: String,
    text: String,
    username: Option[String] = None,
    asUser: Option[Boolean] = None,
    parse: Option[String] = None,
    linkNames: Option[String] = None,
    attachments: Option[Seq[Attachment]] = None,
    blocks: Option[Seq[Block]] = None,
    unfurlLinks: Option[Boolean] = None,
    unfurlMedia: Option[Boolean] = None,
    iconUrl: Option[String] = None,
    iconEmoji: Option[String] = None,
    replaceOriginal: Option[Boolean] = None,
    deleteOriginal: Option[Boolean] = None,
    threadTs: Option[String] = None,
    replyBroadcast: Option[Boolean] = None)(implicit system: ActorSystem): F[String]

  def updateChatMessage(channelId: String, ts: String, text: String,
    attachments: Option[Seq[Attachment]] = None,
    blocks: Option[Seq[Block]] = None,
    parse: Option[String] = None,
    linkNames: Option[String] = None,
    asUser: Option[Boolean] = None,
    threadTs: Option[String] = None)(implicit system: ActorSystem): F[UpdateResponse]


  /***************************/
  /****  Emoji Endpoints  ****/
  /***************************/
  def listEmojis()(implicit system: ActorSystem): F[Map[String, String]]

  /**************************/
  /****  File Endpoints  ****/
  /**************************/
  def deleteFile(fileId: String)(implicit system: ActorSystem): F[Boolean]

  def getFileInfo(fileId: String, count: Option[Int] = None, page: Option[Int] = None)(
    implicit system: ActorSystem
  ): F[FileInfo]

  def listFiles(userId: Option[String] = None,
    tsFrom: Option[String] = None,
    tsTo: Option[String] = None,
    types: Option[Seq[String]] = None,
    count: Option[Int] = None,
    page: Option[Int] = None)(implicit system: ActorSystem): F[FilesResponse]

  def uploadFile(file: File)(implicit system: ActorSystem): F[SlackFile] = {
    uploadFile(Left(file))
  }
  def uploadFile(
      content: Either[File, Array[Byte]],
      filetype: Option[String] = None,
      filename: Option[String] = None,
      title: Option[String] = None,
      initialComment: Option[String] = None,
      channels: Option[Seq[String]] = None,
      thread_ts: Option[String] = None
  )(implicit system: ActorSystem): F[SlackFile]

  /***************************/
  /****  Group Endpoints  ****/
  /***************************/
  def archiveGroup(channelId: String)(implicit system: ActorSystem): F[Boolean]

  def closeGroup(channelId: String)(implicit system: ActorSystem): F[Boolean]

  def createGroup(name: String)(implicit system: ActorSystem): F[Group]

  def createChildGroup(channelId: String)(implicit system: ActorSystem): F[Group]

  def getGroupHistory(channelId: String,
    latest: Option[String] = None,
    oldest: Option[String] = None,
    inclusive: Option[Int] = None,
    count: Option[Int] = None)(implicit system: ActorSystem): F[HistoryChunk]

  def getGroupInfo(channelId: String)(implicit system: ActorSystem): F[Group]
  def inviteToGroup(channelId: String, userId: String)(implicit system: ActorSystem): F[Group]
  def kickFromGroup(channelId: String, userId: String)(implicit system: ActorSystem): F[Boolean]
  def leaveGroup(channelId: String)(implicit system: ActorSystem): F[Boolean]
  def listGroups(excludeArchived: Int = 0)(implicit system: ActorSystem): F[Seq[Group]]
  def markGroup(channelId: String, ts: String)(implicit system: ActorSystem): F[Boolean]
  def openGroup(channelId: String)(implicit system: ActorSystem): F[Boolean]
  // TODO: Lite Group Object
  def renameGroup(channelId: String, name: String)(implicit system: ActorSystem): F[Boolean]
  def setGroupPurpose(channelId: String, purpose: String)(implicit system: ActorSystem): F[String]
  def setGroupTopic(channelId: String, topic: String)(implicit system: ActorSystem): F[String]
  def unarchiveGroup(channelId: String)(implicit system: ActorSystem): F[Boolean]

  /************************/
  /****  IM Endpoints  ****/
  /************************/
  def closeIm(channelId: String)(implicit system: ActorSystem): F[Boolean]

  def getImHistory(channelId: String,
    latest: Option[String] = None,
    oldest: Option[String] = None,
    inclusive: Option[Int] = None,
    count: Option[Int] = None)(implicit system: ActorSystem): F[HistoryChunk]

  def listIms()(implicit system: ActorSystem): F[Seq[Im]]

  def markIm(channelId: String, ts: String)(implicit system: ActorSystem): F[Boolean]

  def openIm(userId: String)(implicit system: ActorSystem): F[String]

  /******************************/
  /****  Reaction Endpoints  ****/
  /******************************/
  def addReaction(emojiName: String,
    file: Option[String] = None,
    fileComment: Option[String] = None,
    channelId: Option[String] = None,
    timestamp: Option[String] = None)(implicit system: ActorSystem): F[Boolean]

  def addReactionToMessage(emojiName: String, channelId: String, timestamp: String)(
    implicit system: ActorSystem
  ): F[Boolean]

  def getReactions(file: Option[String] = None,
    fileComment: Option[String] = None,
    channelId: Option[String] = None,
    timestamp: Option[String] = None,
    full: Option[Boolean] = None)(implicit system: ActorSystem): F[Seq[Reaction]]

  def getReactionsForMessage(channelId: String, timestamp: String, full: Option[Boolean] = None)(
    implicit system: ActorSystem
  ): F[Seq[Reaction]]

  def listReactionsForUser(userId: Option[String],
    full: Boolean = false,
    count: Option[Int] = None,
    page: Option[Int] = None)(implicit system: ActorSystem): F[ReactionsResponse]

  def removeReaction(emojiName: String,
    file: Option[String] = None,
    fileComment: Option[String] = None,
    channelId: Option[String] = None,
    timestamp: Option[String] = None)(implicit system: ActorSystem): F[Boolean]

  def removeReactionFromMessage(emojiName: String, channelId: String, timestamp: String)(
    implicit system: ActorSystem
  ): F[Boolean]

  /*************************/
  /****  RTM Endpoints  ****/
  /*************************/
  def startRealTimeMessageSession()(implicit system: ActorSystem): F[RtmStartState]

  /****************************/
  /****  Search Endpoints  ****/
  /****************************/
  def searchAll(query: String,
    sort: Option[String] = None,
    sortDir: Option[String] = None,
    highlight: Option[String] = None,
    count: Option[Int] = None,
    page: Option[Int] = None)(implicit system: ActorSystem): F[JsValue]

  def searchFiles(query: String,
    sort: Option[String] = None,
    sortDir: Option[String] = None,
    highlight: Option[String] = None,
    count: Option[Int] = None,
    page: Option[Int] = None)(implicit system: ActorSystem): F[JsValue]

  // TODO: Return proper search results (not JsValue)
  def searchMessages(query: String,
    sort: Option[String] = None,
    sortDir: Option[String] = None,
    highlight: Option[String] = None,
    count: Option[Int] = None,
    page: Option[Int] = None)(implicit system: ActorSystem): F[JsValue]

  /***************************/
  /****  Stars Endpoints  ****/
  /***************************/
  def listStars(userId: Option[String] = None, count: Option[Int] = None, page: Option[Int] = None)(
    implicit system: ActorSystem
  ): F[JsValue]

  /**************************/
  /****  Team Endpoints  ****/
  /**************************/
  def getTeamAccessLogs(count: Option[Int], page: Option[Int])(implicit system: ActorSystem): F[JsValue]
  def getTeamInfo()(implicit system: ActorSystem): F[JsValue]

  /**************************/
  /****  User Endpoints  ****/
  /**************************/
  def getUserPresence(userId: String)(implicit system: ActorSystem): F[String]
  def getUserInfo(userId: String)(implicit system: ActorSystem): F[User]
  def listUsers()(implicit system: ActorSystem): F[Seq[User]]
  def setUserActive(userId: String)(implicit system: ActorSystem): F[Boolean]
  def setUserPresence(presence: String)(implicit system: ActorSystem): F[Boolean]
  def lookupUserByEmail(emailId: String)(implicit system: ActorSystem): F[User]
}
