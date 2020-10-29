package slack.api

import java.io.File

import play.api.libs.json.JsValue
import slack.models.{Attachment, AuthIdentity, Block, Channel, Group, Im, Reaction, SlackFile, UpdateResponse, User}

import cats.tagless._

@autoFunctorK
trait SlackApiClientF[F[_]] {

  /**************************/
  /***   Test Endpoints   ***/
  /**************************/
  def test(): F[Boolean]
  def testAuth(): F[AuthIdentity]

  /***************************/
  /***  Channel Endpoints  ***/
  /***************************/
  def archiveChannel(channelId: String): F[Boolean]
  def createChannel(name: String): F[Channel]
  def getChannelHistory(channelId: String,
    latest: Option[String] = None,
    oldest: Option[String] = None,
    inclusive: Option[Int] = None,
    count: Option[Int] = None): F[HistoryChunk]
  def getChannelInfo(channelId: String): F[Channel]
  def inviteToChannel(channelId: String, userId: String): F[Channel]
  def joinChannel(channelId: String): F[Channel]
  def kickFromChannel(channelId: String, userId: String): F[Boolean]
  def listChannels(excludeArchived: Boolean = false): F[Seq[Channel]]
  def leaveChannel(channelId: String): F[Boolean]
  def markChannel(channelId: String, ts: String): F[Boolean]

  // TODO: Lite Channel Object
  def renameChannel(channelId: String, name: String): F[Boolean]
  def getChannelReplies(channelId: String, thread_ts: String): F[RepliesChunk]
  def setChannelPurpose(channelId: String, purpose: String): F[String]
  def setChannelTopic(channelId: String, topic: String): F[String]

  def unarchiveChannel(channelId: String): F[Boolean]

  /**************************/
  /****  Chat Endpoints  ****/
  /**************************/
  def deleteChat(channelId: String, ts: String, asUser: Option[Boolean] = None): F[Boolean]

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
    replyBroadcast: Option[Boolean] = None): F[String]

  def updateChatMessage(channelId: String, ts: String, text: String,
    attachments: Option[Seq[Attachment]] = None,
    blocks: Option[Seq[Block]] = None,
    parse: Option[String] = None,
    linkNames: Option[String] = None,
    asUser: Option[Boolean] = None,
    threadTs: Option[String] = None): F[UpdateResponse]


  /***************************/
  /****  Emoji Endpoints  ****/
  /***************************/
  def listEmojis(): F[Map[String, String]]

  /**************************/
  /****  File Endpoints  ****/
  /**************************/
  def deleteFile(fileId: String): F[Boolean]

  def getFileInfo(fileId: String, count: Option[Int] = None, page: Option[Int] = None): F[FileInfo]

  def listFiles(userId: Option[String] = None,
    tsFrom: Option[String] = None,
    tsTo: Option[String] = None,
    types: Option[Seq[String]] = None,
    count: Option[Int] = None,
    page: Option[Int] = None): F[FilesResponse]

  def uploadFile(file: File): F[SlackFile] = {
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
  ): F[SlackFile]

  /***************************/
  /****  Group Endpoints  ****/
  /***************************/
  def archiveGroup(channelId: String): F[Boolean]

  def closeGroup(channelId: String): F[Boolean]

  def createGroup(name: String): F[Group]

  def createChildGroup(channelId: String): F[Group]

  def getGroupHistory(channelId: String,
    latest: Option[String] = None,
    oldest: Option[String] = None,
    inclusive: Option[Int] = None,
    count: Option[Int] = None): F[HistoryChunk]

  def getGroupInfo(channelId: String): F[Group]
  def inviteToGroup(channelId: String, userId: String): F[Group]
  def kickFromGroup(channelId: String, userId: String): F[Boolean]
  def leaveGroup(channelId: String): F[Boolean]
  def listGroups(excludeArchived: Int = 0): F[Seq[Group]]
  def markGroup(channelId: String, ts: String): F[Boolean]
  def openGroup(channelId: String): F[Boolean]
  // TODO: Lite Group Object
  def renameGroup(channelId: String, name: String): F[Boolean]
  def setGroupPurpose(channelId: String, purpose: String): F[String]
  def setGroupTopic(channelId: String, topic: String): F[String]
  def unarchiveGroup(channelId: String): F[Boolean]

  /************************/
  /****  IM Endpoints  ****/
  /************************/
  def closeIm(channelId: String): F[Boolean]

  def getImHistory(channelId: String,
    latest: Option[String] = None,
    oldest: Option[String] = None,
    inclusive: Option[Int] = None,
    count: Option[Int] = None): F[HistoryChunk]

  def listIms(): F[Seq[Im]]

  def markIm(channelId: String, ts: String): F[Boolean]

  def openIm(userId: String): F[String]

  /******************************/
  /****  Reaction Endpoints  ****/
  /******************************/
  def addReaction(emojiName: String,
    file: Option[String] = None,
    fileComment: Option[String] = None,
    channelId: Option[String] = None,
    timestamp: Option[String] = None): F[Boolean]

  def addReactionToMessage(emojiName: String, channelId: String, timestamp: String): F[Boolean]

  def getReactions(file: Option[String] = None,
    fileComment: Option[String] = None,
    channelId: Option[String] = None,
    timestamp: Option[String] = None,
    full: Option[Boolean] = None): F[Seq[Reaction]]

  def getReactionsForMessage(channelId: String, timestamp: String, full: Option[Boolean] = None): F[Seq[Reaction]]

  def listReactionsForUser(userId: Option[String],
    full: Boolean = false,
    count: Option[Int] = None,
    page: Option[Int] = None): F[ReactionsResponse]

  def removeReaction(emojiName: String,
    file: Option[String] = None,
    fileComment: Option[String] = None,
    channelId: Option[String] = None,
    timestamp: Option[String] = None): F[Boolean]

  def removeReactionFromMessage(emojiName: String, channelId: String, timestamp: String): F[Boolean]

  /*************************/
  /****  RTM Endpoints  ****/
  /*************************/
  def startRealTimeMessageSession(): F[RtmStartState]

  /****************************/
  /****  Search Endpoints  ****/
  /****************************/
  def searchAll(query: String,
    sort: Option[String] = None,
    sortDir: Option[String] = None,
    highlight: Option[String] = None,
    count: Option[Int] = None,
    page: Option[Int] = None): F[JsValue]

  def searchFiles(query: String,
    sort: Option[String] = None,
    sortDir: Option[String] = None,
    highlight: Option[String] = None,
    count: Option[Int] = None,
    page: Option[Int] = None): F[JsValue]

  // TODO: Return proper search results (not JsValue)
  def searchMessages(query: String,
    sort: Option[String] = None,
    sortDir: Option[String] = None,
    highlight: Option[String] = None,
    count: Option[Int] = None,
    page: Option[Int] = None): F[JsValue]

  /***************************/
  /****  Stars Endpoints  ****/
  /***************************/
  def listStars(userId: Option[String] = None, count: Option[Int] = None, page: Option[Int] = None): F[JsValue]

  /**************************/
  /****  Team Endpoints  ****/
  /**************************/
  def getTeamAccessLogs(count: Option[Int], page: Option[Int]): F[JsValue]
  def getTeamInfo(): F[JsValue]

  /**************************/
  /****  User Endpoints  ****/
  /**************************/
  def getUserPresence(userId: String): F[String]
  def getUserInfo(userId: String): F[User]
  def listUsers(): F[Seq[User]]
  def setUserActive(userId: String): F[Boolean]
  def setUserPresence(presence: String): F[Boolean]
  def lookupUserByEmail(emailId: String): F[User]
}
