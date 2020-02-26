package slack.api

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import play.api.libs.json._
import slack.api.SlackApiClient.defaultSlackApiBaseUri
import slack.models._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object BlockingSlackApiClient {

  def apply(token: String,
            slackApiBaseUri: Uri = SlackApiClient.defaultSlackApiBaseUri,
            duration: FiniteDuration = 5.seconds): BlockingSlackApiClient = {
    new BlockingSlackApiClient(token, slackApiBaseUri, duration)
  }

  def exchangeOauthForToken(
    clientId: String,
    clientSecret: String,
    code: String,
    redirectUri: Option[String] = None,
    duration: FiniteDuration = 5.seconds,
    slackApiBaseUri: Uri = defaultSlackApiBaseUri
  )(implicit system: ActorSystem): AccessToken = {
    Await.result(
      SlackApiClient.exchangeOauthForToken(clientId, clientSecret, code, redirectUri, slackApiBaseUri),
      duration
    )
  }
}

class BlockingSlackApiClient private (token: String, slackApiBaseUri: Uri, duration: FiniteDuration) {
  val client = SlackApiClient(token, slackApiBaseUri)

  /**************************/
  /***   Test Endpoints   ***/
  /**************************/
  def test()(implicit system: ActorSystem): Boolean = {
    resolve(client.test())
  }

  def testAuth()(implicit system: ActorSystem): AuthIdentity = {
    resolve(client.testAuth())
  }

  /***************************/
  /***  Channel Endpoints  ***/
  /***************************/
  def archiveChannel(channelId: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.archiveChannel(channelId))
  }

  def createChannel(name: String)(implicit system: ActorSystem): Channel = {
    resolve(client.createChannel(name))
  }

  def getChannelHistory(channelId: String,
                        latest: Option[String] = None,
                        oldest: Option[String] = None,
                        inclusive: Option[Int] = None,
                        count: Option[Int] = None)(implicit system: ActorSystem): HistoryChunk = {
    resolve(client.getChannelHistory(channelId, latest, oldest, inclusive, count))
  }

  def getChannelInfo(channelId: String)(implicit system: ActorSystem): Channel = {
    resolve(client.getChannelInfo(channelId))
  }

  def inviteToChannel(channelId: String, userId: String)(implicit system: ActorSystem): Channel = {
    resolve(client.inviteToChannel(channelId, userId))
  }

  def joinChannel(channelId: String)(implicit system: ActorSystem): Channel = {
    resolve(client.joinChannel(channelId))
  }

  def kickFromChannel(channelId: String, userId: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.kickFromChannel(channelId, userId))
  }

  def listChannels(excludeArchived: Int = 0)(implicit system: ActorSystem): Seq[Channel] = {
    resolve(client.listChannels(excludeArchived))
  }

  def leaveChannel(channelId: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.leaveChannel(channelId))
  }

  def markChannel(channelId: String, ts: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.markChannel(channelId, ts))
  }

  // TODO: Lite Channel Object
  def renameChannel(channelId: String, name: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.renameChannel(channelId, name))
  }

  def getChannelReplies(channelId: String, thread_ts: String)(implicit system: ActorSystem): RepliesChunk = {
    resolve(client.getChannelReplies(channelId, thread_ts))
  }

  def setChannelPurpose(channelId: String, purpose: String)(implicit system: ActorSystem): String = {
    resolve(client.setChannelPurpose(channelId, purpose))
  }

  def setChannelTopic(channelId: String, topic: String)(implicit system: ActorSystem): String = {
    resolve(client.setChannelTopic(channelId, topic))
  }

  def unarchiveChannel(channelId: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.unarchiveChannel(channelId))
  }

  /**************************/
  /****  Chat Endpoints  ****/
  /**************************/
  def deleteChat(channelId: String, ts: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.deleteChat(channelId, ts))
  }

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
                      replyBroadcast: Option[Boolean] = None)(implicit system: ActorSystem): String = {
    resolve(
      client.postChatMessage(
        channelId,
        text,
        username,
        asUser,
        parse,
        linkNames,
        attachments,
        blocks,
        unfurlLinks,
        unfurlMedia,
        iconUrl,
        iconEmoji,
        replaceOriginal,
        deleteOriginal,
        threadTs,
        replyBroadcast
      )
    )
  }

  def updateChatMessage(channelId: String, ts: String, text: String,
                        attachments: Option[Seq[Attachment]] = None,
                        blocks: Option[Seq[Block]] = None,
                        parse: Option[String] = None,
                        linkNames: Option[String] = None,
                        asUser: Option[Boolean] = None,
                        threadTs: Option[String] = None)(implicit system: ActorSystem): UpdateResponse = {
    resolve(client.updateChatMessage(channelId, ts, text, attachments, blocks, parse, linkNames, asUser, threadTs))

  }

  /***************************/
  /****  Emoji Endpoints  ****/
  /***************************/
  def listEmojis()(implicit system: ActorSystem): Map[String, String] = {
    resolve(client.listEmojis())
  }

  /**************************/
  /****  File Endpoints  ****/
  /**************************/
  def deleteFile(fileId: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.deleteFile(fileId))
  }

  def getFileInfo(fileId: String, count: Option[Int] = None, page: Option[Int] = None)(
    implicit system: ActorSystem
  ): FileInfo = {
    resolve(client.getFileInfo(fileId, count, page))
  }

  def listFiles(userId: Option[String] = None,
                tsFrom: Option[String] = None,
                tsTo: Option[String] = None,
                types: Option[Seq[String]] = None,
                count: Option[Int] = None,
                page: Option[Int] = None)(implicit system: ActorSystem): FilesResponse = {
    resolve(client.listFiles(userId, tsFrom, tsTo, types, count, page))
  }

  def uploadFile(file: File)(implicit system: ActorSystem): SlackFile = {
    resolve(client.uploadFile(Left(file)))
  }

  /***************************/
  /****  Group Endpoints  ****/
  /***************************/
  def archiveGroup(channelId: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.archiveGroup(channelId))
  }

  def closeGroup(channelId: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.closeGroup(channelId))
  }

  def createGroup(name: String)(implicit system: ActorSystem): Group = {
    resolve(client.createGroup(name))
  }

  def createChildGroup(channelId: String)(implicit system: ActorSystem): Group = {
    resolve(client.createChildGroup(channelId))
  }

  def getGroupHistory(channelId: String,
                      latest: Option[String] = None,
                      oldest: Option[String] = None,
                      inclusive: Option[Int] = None,
                      count: Option[Int] = None)(implicit system: ActorSystem): HistoryChunk = {
    resolve(client.getGroupHistory(channelId, latest, oldest, inclusive, count))
  }

  def getGroupInfo(channelId: String)(implicit system: ActorSystem): Group = {
    resolve(client.getGroupInfo(channelId))
  }

  def inviteToGroup(channelId: String, userId: String)(implicit system: ActorSystem): Group = {
    resolve(client.inviteToGroup(channelId, userId))
  }

  def kickFromGroup(channelId: String, userId: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.kickFromGroup(channelId, userId))
  }

  def leaveGroup(channelId: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.leaveGroup(channelId))
  }

  def listGroups(excludeArchived: Int = 0)(implicit system: ActorSystem): Seq[Group] = {
    resolve(client.listGroups(excludeArchived))
  }

  def markGroup(channelId: String, ts: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.markGroup(channelId, ts))
  }

  def openGroup(channelId: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.openGroup(channelId))
  }

  // TODO: Lite Group Object
  def renameGroup(channelId: String, name: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.renameGroup(channelId, name))
  }

  def setGroupPurpose(channelId: String, purpose: String)(implicit system: ActorSystem): String = {
    resolve(client.setGroupPurpose(channelId, purpose))
  }

  def setGroupTopic(channelId: String, topic: String)(implicit system: ActorSystem): String = {
    resolve(client.setGroupTopic(channelId, topic))
  }

  def unarchiveGroup(channelId: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.unarchiveGroup(channelId))
  }

  /************************/
  /****  IM Endpoints  ****/
  /************************/
  def closeIm(channelId: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.closeIm(channelId))
  }

  def getImHistory(channelId: String,
                   latest: Option[String] = None,
                   oldest: Option[String] = None,
                   inclusive: Option[Int] = None,
                   count: Option[Int] = None)(implicit system: ActorSystem): HistoryChunk = {
    resolve(client.getImHistory(channelId, latest, oldest, inclusive, count))
  }

  def listIms()(implicit system: ActorSystem): Seq[Im] = {
    resolve(client.listIms())
  }

  def markIm(channelId: String, ts: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.markIm(channelId, ts))
  }

  def openIm(userId: String)(implicit system: ActorSystem): String = {
    resolve(client.openIm(userId))
  }

  /******************************/
  /****  Reaction Endpoints  ****/
  /******************************/
  def addReaction(emojiName: String,
                  file: Option[String] = None,
                  fileComment: Option[String] = None,
                  channelId: Option[String] = None,
                  timestamp: Option[String] = None)(implicit system: ActorSystem): Boolean = {
    resolve(client.addReaction(emojiName, file, fileComment, channelId, timestamp))
  }

  def addReactionToMessage(emojiName: String, channelId: String, timestamp: String)(
    implicit system: ActorSystem
  ): Boolean = {
    resolve(client.addReactionToMessage(emojiName, channelId, timestamp))
  }

  def getReactions(file: Option[String] = None,
                   fileComment: Option[String] = None,
                   channelId: Option[String] = None,
                   timestamp: Option[String] = None,
                   full: Option[Boolean] = None)(implicit system: ActorSystem): Seq[Reaction] = {
    resolve(client.getReactions(file, fileComment, channelId, timestamp, full))
  }

  def getReactionsForMessage(channelId: String, timestamp: String, full: Option[Boolean] = None)(
    implicit system: ActorSystem
  ): Seq[Reaction] = {
    resolve(client.getReactionsForMessage(channelId, timestamp, full))
  }

  def listReactionsForUser(userId: Option[String],
                           full: Boolean = false,
                           count: Option[Int] = None,
                           page: Option[Int] = None)(implicit system: ActorSystem): ReactionsResponse = {
    resolve(client.listReactionsForUser(userId, full, count, page))
  }

  def removeReaction(emojiName: String,
                     file: Option[String] = None,
                     fileComment: Option[String] = None,
                     channelId: Option[String] = None,
                     timestamp: Option[String] = None)(implicit system: ActorSystem): Boolean = {
    resolve(client.removeReaction(emojiName, file, fileComment, channelId, timestamp))
  }

  def removeReactionFromMessage(emojiName: String, channelId: String, timestamp: String)(
    implicit system: ActorSystem
  ): Boolean = {
    resolve(client.removeReactionFromMessage(emojiName, channelId, timestamp))
  }

  /*************************/
  /****  RTM Endpoints  ****/
  /*************************/
  def startRealTimeMessageSession()(implicit system: ActorSystem): RtmStartState = {
    resolve(client.startRealTimeMessageSession())
  }

  /****************************/
  /****  Search Endpoints  ****/
  /****************************/
  def searchAll(query: String,
                sort: Option[String] = None,
                sortDir: Option[String] = None,
                highlight: Option[String] = None,
                count: Option[Int] = None,
                page: Option[Int] = None)(implicit system: ActorSystem): JsValue = {
    resolve(client.searchAll(query, sort, sortDir, highlight, count, page))
  }

  def searchFiles(query: String,
                  sort: Option[String] = None,
                  sortDir: Option[String] = None,
                  highlight: Option[String] = None,
                  count: Option[Int] = None,
                  page: Option[Int] = None)(implicit system: ActorSystem): JsValue = {
    resolve(client.searchFiles(query, sort, sortDir, highlight, count, page))
  }

  // TODO: Return proper search results (not JsValue)
  def searchMessages(query: String,
                     sort: Option[String] = None,
                     sortDir: Option[String] = None,
                     highlight: Option[String] = None,
                     count: Option[Int] = None,
                     page: Option[Int] = None)(implicit system: ActorSystem): JsValue = {
    resolve(client.searchMessages(query, sort, sortDir, highlight, count, page))
  }

  /***************************/
  /****  Stars Endpoints  ****/
  /***************************/
  def listStars(userId: Option[String] = None, count: Option[Int] = None, page: Option[Int] = None)(
    implicit system: ActorSystem
  ): JsValue = {
    resolve(client.listStars(userId, count, page))
  }

  /**************************/
  /****  Team Endpoints  ****/
  /**************************/
  def getTeamAccessLogs(count: Option[Int], page: Option[Int])(implicit system: ActorSystem): JsValue = {
    resolve(client.getTeamAccessLogs(count, page))
  }

  def getTeamInfo()(implicit system: ActorSystem): JsValue = {
    resolve(client.getTeamInfo())
  }

  /**************************/
  /****  User Endpoints  ****/
  /**************************/
  def getUserPresence(userId: String)(implicit system: ActorSystem): String = {
    resolve(client.getUserPresence(userId))
  }

  def getUserInfo(userId: String)(implicit system: ActorSystem): User = {
    resolve(client.getUserInfo(userId))
  }

  def listUsers()(implicit system: ActorSystem): Seq[User] = {
    resolve(client.listUsers())
  }

  def setUserActive(userId: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.setUserActive(userId))
  }

  def setUserPresence(presence: String)(implicit system: ActorSystem): Boolean = {
    resolve(client.setUserPresence(presence))
  }

  def lookupUserByEmail(emailId: String)(implicit system: ActorSystem): User = {
    resolve(client.lookupUserByEmail(emailId))
  }

  private def resolve[T](res: Future[T]): T = {
    Await.result(res, duration)
  }
}
