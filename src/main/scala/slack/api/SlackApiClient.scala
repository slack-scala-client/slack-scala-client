package slack.api

import slack.models._

import java.io.File
import scala.concurrent.ExecutionContext

import dispatch._
import play.api.libs.json._

object SlackApiClient {

  implicit val channelHistoryChunkFmt = Json.format[ChannelHistoryChunk]
  implicit val groupHistoryChunkFmt = Json.format[GroupHistoryChunk]
  implicit val pagingObjectFmt = Json.format[PagingObject]
  implicit val filesResponseFmt = Json.format[FilesResponse]
  implicit val fileInfoFmt = Json.format[FileInfo]

  def apply(token: String): SlackApiClient = {
    new SlackApiClient(token)
  }
}

import SlackApiClient._

class SlackApiClient(token: String) {

  val apiBase = url("https://slack.com/api").addQueryParameter("token", token)


  /**************************/
  /***   Test Endpoints   ***/
  /**************************/

  def test()(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("api.test")
    extract[Boolean](res, "ok")
  }

  def testAuth()(implicit ec: ExecutionContext): Future[AuthIdentity] = {
    val res = makeApiRequest("auth.test")
    res.map(_.as[AuthIdentity])
  }


  /***************************/
  /***  Channel Endpoints  ***/
  /***************************/

  def archiveChannel(channelId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("channels.archive", ("channel" -> channelId))
    extract[Boolean](res, "ok")
  }

  def createChannel(name: String)(implicit ec: ExecutionContext): Future[Channel] = {
    val res = makeApiRequest("channels.create", ("name" -> name))
    extract[Channel](res, "channel")
  }

  // TODO: Paging
  def getChannelHistory(channelId: String, latest: Option[Long] = None, oldest: Option[Long] = None,
      inclusive: Option[Int] = None, count: Option[Int] = None)(implicit ec: ExecutionContext): Future[ChannelHistoryChunk] = {
    var params = createParams (
      ("channel" -> channelId),
      ("latest" -> latest),
      ("oldest" -> oldest),
      ("inclusive" -> inclusive),
      ("count" -> count)
    )
    val res = makeApiRequest("channels.history", params: _*)
    res.map(_.as[ChannelHistoryChunk])
  }

  def getChannelInfo(channelId: String)(implicit ec: ExecutionContext): Future[Channel] = {
    val res = makeApiRequest("channels.info", ("channel" -> channelId))
    extract[Channel](res, "channel")
  }

  def inviteToChannel(channelId: String, userId: String)(implicit ec: ExecutionContext): Future[Channel] = {
    val res = makeApiRequest("channels.invite", ("channel" -> channelId), ("user" -> userId))
    extract[Channel](res, "channel")
  }

  def joinChannel(channelId: String)(implicit ec: ExecutionContext): Future[Channel] = {
    val res = makeApiRequest("channels.join", ("channel" -> channelId))
    extract[Channel](res, "channel")
  }

  def kickFromChannel(channelId: String, userId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("channels.kick", ("channel" -> channelId), ("user" -> userId))
    extract[Boolean](res, "ok")
  }

  def listChannels(excludeArchived: Int = 0)(implicit ec: ExecutionContext): Future[Seq[Channel]] = {
    val res = makeApiRequest("channels.list", ("exclude_archived" -> excludeArchived.toString))
    extract[Seq[Channel]](res, "channels")
  }

  def leaveChannel(channelId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("channels.leave", ("channel" -> channelId))
    extract[Boolean](res, "ok")
  }

  def markChannel(channelId: String, ts: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("channels.mark", ("channel" -> channelId), ("ts" -> ts))
    extract[Boolean](res, "ok")
  }

  // TODO: Lite Channel Object
  def renameChannel(channelId: String, name: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("channels.rename", ("channel" -> channelId), ("name" -> name))
    extract[Boolean](res, "ok")
  }

  def setChannelPurpose(channelId: String, purpose: String)(implicit ec: ExecutionContext): Future[String] = {
    val res = makeApiRequest("channels.setPurpose", ("channel" -> channelId), ("purpose" -> purpose))
    extract[String](res, "purpose")
  }

  def setChannelTopic(channelId: String, topic: String)(implicit ec: ExecutionContext): Future[String] = {
    val res = makeApiRequest("channels.setTopic", ("channel" -> channelId), ("topic" -> topic))
    extract[String](res, "topic")
  }

  def unarchiveChannel(channelId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("channels.unarchive", ("channel" -> channelId))
    extract[Boolean](res, "ok")
  }


  /**************************/
  /****  Chat Endpoints  ****/
  /**************************/

  def deleteChat(channelId: String, ts: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("chats.delete", ("channel" -> channelId), ("ts" -> ts))
    extract[Boolean](res, "ok")
  }

  def postChatMessage(channelId: String, text: String)(implicit ec: ExecutionContext): Future[String] = {
    val res = makeApiRequest("chats.postMessage", ("channel" -> channelId), ("text" -> text))
    extract[String](res, "ts")
  }

  def postChatMessageFull(channelId: String, message: ChatMessage)(implicit ec: ExecutionContext): Future[String] = {
    var params = createParams (
      ("channel" -> channelId),
      ("text" -> message.text),
      ("as_user" -> message.as_user),
      ("parse" -> message.parse),
      ("link_names" -> message.link_names),
      ("attachements" -> message.attachements),
      ("unfurl_links" -> message.unfurl_links),
      ("unfurl_media" -> message.unfurl_media),
      ("icon_url" -> message.icon_url),
      ("icon_emoji" -> message.icon_emoji)
    )
    val res = makeApiRequest("chats.postMessage", params: _*)
    extract[String](res, "ts")
  }

  def updateChatMessage(channelId: String, ts: String, text: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("chats.update", ("channel" -> channelId), ("ts" -> ts), ("text" -> text))
    extract[Boolean](res, "ok")
  }


  /***************************/
  /****  Emoji Endpoints  ****/
  /***************************/

  def listEmojis()(implicit ec: ExecutionContext): Future[Map[String,String]] = {
    val res = makeApiRequest("emoji.list")
    extract[Map[String,String]](res, "emoji")
  }


  /**************************/
  /****  File Endpoints  ****/
  /**************************/

  def deleteFile(fileId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("files.delete", ("file" -> fileId))
    extract[Boolean](res, "ok")
  }

  def getFileInfo(fileId: String, count: Option[Int] = None, page: Option[Int] = None)(implicit ec: ExecutionContext): Future[FileInfo] = {
    var params = createParams (
      ("file" -> fileId),
      ("count" -> count),
      ("page" -> page)
    )
    val res = makeApiRequest("files.info", params: _*)
    res.map(_.as[FileInfo])
  }

  def listFiles(userId: Option[String] = None, tsFrom: Option[String] = None, tsTo: Option[String] = None, types: Option[Seq[String]] = None,
      count: Option[Int] = None, page: Option[Int] = None)(implicit ec: ExecutionContext): Future[FilesResponse] = {
    var params = createParams (
      ("user" -> userId),
      ("ts_from" -> tsFrom),
      ("ts_to" -> tsTo),
      ("types" -> types.map(_.mkString(","))),
      ("count" -> count),
      ("page" -> page)
    )
    val res = makeApiRequest("files.list", params: _*)
    res.map(_.as[FilesResponse])
  }

  def uploadFile(file: File)(implicit ec: ExecutionContext): Future[File] = {
    ??? // TODO
  }


  /***************************/
  /****  Group Endpoints  ****/
  /***************************/

  def archiveGroup(channelId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("groups.archive", ("channel" -> channelId))
    extract[Boolean](res, "ok")
  }

  def closeGroup(channelId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("groups.close", ("channel" -> channelId))
    extract[Boolean](res, "ok")
  }

  def createGroup(name: String)(implicit ec: ExecutionContext): Future[Group] = {
    val res = makeApiRequest("groups.create", ("name" -> name))
    extract[Group](res, "group")
  }

  def createChildGroup(channelId: String)(implicit ec: ExecutionContext): Future[Group] = {
    val res = makeApiRequest("groups.createChild", ("channel" -> channelId))
    extract[Group](res, "group")
  }

  def getGroupHistory(channelId: String, latest: Option[Long] = None, oldest: Option[Long] = None,
      inclusive: Option[Int] = None, count: Option[Int] = None)(implicit ec: ExecutionContext): Future[GroupHistoryChunk] = {
    var params = createParams (
      ("channel" -> channelId),
      ("latest" -> latest),
      ("oldest" -> oldest),
      ("inclusive" -> inclusive),
      ("count" -> count)
    )
    val res = makeApiRequest("groups.history", params: _*)
    res.map(_.as[GroupHistoryChunk])
  }

  def getGroupInfo(channelId: String)(implicit ec: ExecutionContext): Future[Group] = {
    val res = makeApiRequest("groups.info", ("channel" -> channelId))
    extract[Group](res, "group")
  }

  def inviteToGroup(channelId: String, userId: String)(implicit ec: ExecutionContext): Future[Group] = {
    val res = makeApiRequest("groups.invite", ("channel" -> channelId), ("user" -> userId))
    extract[Group](res, "group")
  }

  def kickFromGroup(channelId: String, userId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("groups.kick", ("channel" -> channelId), ("user" -> userId))
    extract[Boolean](res, "ok")
  }

  def leaveGroup(channelId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("groups.leave", ("channel" -> channelId))
    extract[Boolean](res, "ok")
  }

  def listGroups(excludeArchived: Int = 0)(implicit ec: ExecutionContext): Future[Seq[Group]] = {
    val res = makeApiRequest("groups.list", ("exclude_archived" -> excludeArchived.toString))
    extract[Seq[Group]](res, "groups")
  }

  def markGroup(channelId: String, ts: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("groups.mark", ("channel" -> channelId), ("ts" -> ts))
    extract[Boolean](res, "ok")
  }

  def openGroup(channelId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("groups.open", ("channel" -> channelId))
    extract[Boolean](res, "ok")
  }

  // TODO: Lite Group Object
  def renameGroup(channelId: String, name: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("groups.rename", ("channel" -> channelId), ("name" -> name))
    extract[Boolean](res, "ok")
  }

  def setGroupPurpose(channelId: String, purpose: String)(implicit ec: ExecutionContext): Future[String] = {
    val res = makeApiRequest("groups.setPurpose", ("channel" -> channelId), ("purpose" -> purpose))
    extract[String](res, "purpose")
  }

  def setGroupTopic(channelId: String, topic: String)(implicit ec: ExecutionContext): Future[String] = {
    val res = makeApiRequest("groups.setTopic", ("channel" -> channelId), ("topic" -> topic))
    extract[String](res, "topic")
  }

  def unarchiveGroup(channelId: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("groups.unarchive", ("channel" -> channelId))
    extract[Boolean](res, "ok")
  }


  /*****************************/
  /****  Private Funstions  ****/
  /*****************************/

  private def makeApiRequest(apiMethod: String, queryParams: (String,String)*)(implicit ec: ExecutionContext): Future[JsValue] = {
    var req = apiBase / apiMethod
    queryParams.foreach { case (k,v) =>
      req = req.addQueryParameter(k, v)
    }
    makeApiRequest(req)
  }

  private def makeApiRequest(request: Req)(implicit ec: ExecutionContext): Future[JsValue] = {
    Http(request OK as.String).map { response =>
      val parsed = Json.parse(response)
      val ok = (parsed \ "ok").as[Boolean]
      if(ok) {
        parsed
      } else {
        throw new ApiError((parsed \ "error").as[String])
      }
    }
  }

  private def extract[T](jsFuture: Future[JsValue], field: String)(implicit ec: ExecutionContext, fmt: Format[T]): Future[T] = {
    jsFuture.map(js => (js \ field).as[T])
  }

  private def createParams(params: (String,Any)*): Seq[(String,String)] = {
    var paramList = Seq[(String,String)]()
    params.foreach {
      case (k, Some(v)) => paramList :+= (k -> v.toString)
      case (k, None) => // Nothing - Filter out none
      case (k, v) => paramList :+= (k -> v.toString)
    }
    paramList
  }
}

case class ApiError(code: String) extends Exception(code)

case class ChannelHistoryChunk (
  latest: Long,
  messages: Seq[JsValue],
  has_more: Boolean
) // TODO: Message

case class GroupHistoryChunk (
  latest: Long,
  messages: Seq[JsValue],
  has_more: Boolean
)

case class ChatMessage (
  text: String,
  username: Option[String],
  as_user: Option[Boolean],
  parse: Option[String],
  link_names: Option[String],
  attachements: Option[Seq[JsValue]],
  unfurl_links: Option[Boolean],
  unfurl_media: Option[Boolean],
  icon_url: Option[String],
  icon_emoji: Option[String]
)

case class FileInfo (
  file: SlackFile,
  comments: Seq[SlackComment],
  paging: PagingObject
)

case class FilesResponse (
  files: Seq[SlackFile],
  paging: PagingObject
)

case class PagingObject (
  count: Int,
  total: Int,
  page: Int,
  pages: Int
)