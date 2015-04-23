package slack.api

import slack.models._

import java.io.File
import scala.concurrent.{ExecutionContext,Future,Await}
import scala.concurrent.duration._

import play.api.libs.json._

object BlockingSlackApiClient {

  def apply(token: String, duration: FiniteDuration): BlockingSlackApiClient = {
    new BlockingSlackApiClient(token, duration)
  }

  def exchangeOauthForToken(clientId: String, clientSecret: String, code: String, redirectUri: Option[String] = None, 
      duration: FiniteDuration = 5.seconds)(implicit ec: ExecutionContext): AccessToken = {
    Await.result(SlackApiClient.exchangeOauthForToken(clientId, clientSecret, code, redirectUri), duration)
  }
}

import SlackApiClient._

class BlockingSlackApiClient(token: String, duration: FiniteDuration = 5.seconds) {
  val client = new SlackApiClient(token)

    /**************************/
  /***   Test Endpoints   ***/
  /**************************/

  def test()(implicit ec: ExecutionContext): Boolean = {
    resolve(client.test())
  }

  def testAuth()(implicit ec: ExecutionContext): AuthIdentity = {
    resolve(client.testAuth())
  }


  /***************************/
  /***  Channel Endpoints  ***/
  /***************************/

  def archiveChannel(channelId: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.archiveChannel(channelId))
  }

  def createChannel(name: String)(implicit ec: ExecutionContext): Channel = {
    resolve(client.createChannel(name))
  }

  def getChannelHistory(channelId: String, latest: Option[Long] = None, oldest: Option[Long] = None,
      inclusive: Option[Int] = None, count: Option[Int] = None)(implicit ec: ExecutionContext): HistoryChunk = {
    resolve(client.getChannelHistory(channelId, latest, oldest, inclusive, count))
  }

  def getChannelInfo(channelId: String)(implicit ec: ExecutionContext): Channel = {
    resolve(client.getChannelInfo(channelId))
  }

  def inviteToChannel(channelId: String, userId: String)(implicit ec: ExecutionContext): Channel = {
    resolve(client.inviteToChannel(channelId, userId))
  }

  def joinChannel(channelId: String)(implicit ec: ExecutionContext): Channel = {
    resolve(client.joinChannel(channelId))
  }

  def kickFromChannel(channelId: String, userId: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.kickFromChannel(channelId, userId))
  }

  def listChannels(excludeArchived: Int = 0)(implicit ec: ExecutionContext): Seq[Channel] = {
    resolve(client.listChannels(excludeArchived))
  }

  def leaveChannel(channelId: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.leaveChannel(channelId))
  }

  def markChannel(channelId: String, ts: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.markChannel(channelId, ts))
  }

  // TODO: Lite Channel Object
  def renameChannel(channelId: String, name: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.renameChannel(channelId, name))
  }

  def setChannelPurpose(channelId: String, purpose: String)(implicit ec: ExecutionContext): String = {
    resolve(client.setChannelPurpose(channelId, purpose))
  }

  def setChannelTopic(channelId: String, topic: String)(implicit ec: ExecutionContext): String = {
    resolve(client.setChannelTopic(channelId, topic))
  }

  def unarchiveChannel(channelId: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.unarchiveChannel(channelId))
  }


  /**************************/
  /****  Chat Endpoints  ****/
  /**************************/

  def deleteChat(channelId: String, ts: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.deleteChat(channelId, ts))
  }

  def postChatMessage(channelId: String, text: String)(implicit ec: ExecutionContext): String = {
    resolve(client.postChatMessage(channelId, text))
  }

  def postChatMessageFull(channelId: String, message: ChatMessage)(implicit ec: ExecutionContext): String = {
    resolve(client.postChatMessageFull(channelId, message))
  }

  def updateChatMessage(channelId: String, ts: String, text: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.updateChatMessage(channelId, ts, text))
  }


  /***************************/
  /****  Emoji Endpoints  ****/
  /***************************/

  def listEmojis()(implicit ec: ExecutionContext): Map[String,String] = {
    resolve(client.listEmojis())
  }


  /**************************/
  /****  File Endpoints  ****/
  /**************************/

  def deleteFile(fileId: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.deleteFile(fileId))
  }

  def getFileInfo(fileId: String, count: Option[Int] = None, page: Option[Int] = None)(implicit ec: ExecutionContext): FileInfo = {
    resolve(client.getFileInfo(fileId, count, page))
  }

  def listFiles(userId: Option[String] = None, tsFrom: Option[String] = None, tsTo: Option[String] = None, types: Option[Seq[String]] = None,
      count: Option[Int] = None, page: Option[Int] = None)(implicit ec: ExecutionContext): FilesResponse = {
    resolve(client.listFiles(userId, tsFrom, tsTo, types, count, page))
  }

  def uploadFile(file: File)(implicit ec: ExecutionContext): SlackFile = {
    resolve(client.uploadFile(file))
  }


  /***************************/
  /****  Group Endpoints  ****/
  /***************************/

  def archiveGroup(channelId: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.archiveGroup(channelId))
  }

  def closeGroup(channelId: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.closeGroup(channelId))
  }

  def createGroup(name: String)(implicit ec: ExecutionContext): Group = {
    resolve(client.createGroup(name))
  }

  def createChildGroup(channelId: String)(implicit ec: ExecutionContext): Group = {
    resolve(client.createChildGroup(channelId))
  }

  def getGroupHistory(channelId: String, latest: Option[Long] = None, oldest: Option[Long] = None,
      inclusive: Option[Int] = None, count: Option[Int] = None)(implicit ec: ExecutionContext): HistoryChunk = {
    resolve(client.getGroupHistory(channelId, latest, oldest, inclusive, count))
  }

  def getGroupInfo(channelId: String)(implicit ec: ExecutionContext): Group = {
    resolve(client.getGroupInfo(channelId))
  }

  def inviteToGroup(channelId: String, userId: String)(implicit ec: ExecutionContext): Group = {
    resolve(client.inviteToGroup(channelId, userId))
  }

  def kickFromGroup(channelId: String, userId: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.kickFromGroup(channelId, userId))
  }

  def leaveGroup(channelId: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.leaveGroup(channelId))
  }

  def listGroups(excludeArchived: Int = 0)(implicit ec: ExecutionContext): Seq[Group] = {
    resolve(client.listGroups(excludeArchived))
  }

  def markGroup(channelId: String, ts: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.markGroup(channelId, ts))
  }

  def openGroup(channelId: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.openGroup(channelId))
  }

  // TODO: Lite Group Object
  def renameGroup(channelId: String, name: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.renameGroup(channelId, name))
  }

  def setGroupPurpose(channelId: String, purpose: String)(implicit ec: ExecutionContext): String = {
    resolve(client.setGroupPurpose(channelId, purpose))
  }

  def setGroupTopic(channelId: String, topic: String)(implicit ec: ExecutionContext): String = {
    resolve(client.setGroupTopic(channelId, topic))
  }

  def unarchiveGroup(channelId: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.unarchiveGroup(channelId))
  }

  /************************/
  /****  IM Endpoints  ****/
  /************************/

  def closeIm(channelId: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.closeIm(channelId))
  }

  def getImHistory(channelId: String, latest: Option[Long] = None, oldest: Option[Long] = None,
      inclusive: Option[Int] = None, count: Option[Int] = None)(implicit ec: ExecutionContext): HistoryChunk = {
    resolve(client.getImHistory(channelId, latest, oldest, inclusive, count))
  }

  def listIms()(implicit ec: ExecutionContext): Seq[Im] = {
    resolve(client.listIms())
  }

  def markIm(channelId: String, ts: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.markIm(channelId, ts))
  }

  def openIm(userId: String)(implicit ec: ExecutionContext): String = {
    resolve(client.openIm(userId))
  }

  /*************************/
  /****  RTM Endpoints  ****/
  /*************************/

  def startRealTimeMessageSession()(implicit ec: ExecutionContext): RtmStartState = {
    resolve(client.startRealTimeMessageSession())
  }

  /****************************/
  /****  Search Endpoints  ****/
  /****************************/

  def searchAll(query: String, sort: Option[String] = None, sortDir: Option[String] = None, highlight: Option[String] = None,
      count: Option[Int] = None, page: Option[Int] = None)(implicit ec: ExecutionContext): JsValue = {
    resolve(client.searchAll(query, sort, sortDir, highlight, count, page))
  }

  def searchFiles(query: String, sort: Option[String] = None, sortDir: Option[String] = None, highlight: Option[String] = None,
      count: Option[Int] = None, page: Option[Int] = None)(implicit ec: ExecutionContext): JsValue = {
    resolve(client.searchFiles(query, sort, sortDir, highlight, count, page))
  }

  // TODO: Return proper search results (not JsValue)
  def searchMessages(query: String, sort: Option[String] = None, sortDir: Option[String] = None, highlight: Option[String] = None,
      count: Option[Int] = None, page: Option[Int] = None)(implicit ec: ExecutionContext): JsValue = {
    resolve(client.searchMessages(query, sort, sortDir, highlight, count, page))
  }


  /***************************/
  /****  Stars Endpoints  ****/
  /***************************/

  def listStars(userId: Option[String] = None, count: Option[Int] = None, page: Option[Int] = None)(implicit ec: ExecutionContext): JsValue = {
    resolve(client.listStars(userId, count, page))
  }


  /**************************/
  /****  Team Endpoints  ****/
  /**************************/

  def getTeamAccessLogs(count: Option[Int], page: Option[Int])(implicit ec: ExecutionContext): JsValue = {
    resolve(client.getTeamAccessLogs(count, page))
  }

  def getTeamInfo()(implicit ec: ExecutionContext): JsValue = {
    resolve(client.getTeamInfo())
  }


  /**************************/
  /****  User Endpoints  ****/
  /**************************/

  def getUserPresence(userId: String)(implicit ec: ExecutionContext): String = {
    resolve(client.getUserPresence(userId))
  }

  def getUserInfo(userId: String)(implicit ec: ExecutionContext): User = {
    resolve(client.getUserInfo(userId))
  }

  def listUsers()(implicit ec: ExecutionContext): Seq[User] = {
    resolve(client.listUsers())
  }

  def setUserActive(userId: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.setUserActive(userId))
  }

  def setUserPresence(presence: String)(implicit ec: ExecutionContext): Boolean = {
    resolve(client.setUserPresence(presence))
  }

  private def resolve[T](res: Future[T]): T = {
    Await.result(res, duration)
  }
}