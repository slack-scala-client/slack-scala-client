package slack.api

import slack.models._

import scala.concurrent.ExecutionContext

import dispatch._
import play.api.libs.json._

object SlackApiClient {

  implicit val historyChunkFmt = Json.format[HistoryChunk]

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
  def channelHistory(channelId: String, latest: Option[Long] = None, oldest: Option[Long] = None,
      inclusive: Option[Int] = None, count: Option[Int] = None)(implicit ec: ExecutionContext): Future[HistoryChunk] = {
    var params = createParams (
      ("channel" -> channelId),
      ("latest" -> latest),
      ("oldest" -> oldest),
      ("inclusive" -> inclusive),
      ("count" -> count)
    )
    val res = makeApiRequest("channels.history", params: _*)
    res.map(_.as[HistoryChunk])
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
    val res = makeApiRequest("channels.invite", ("channel" -> channelId), ("user" -> userId))
    extract[Boolean](res, "ok")
  }

  def listChannels()(implicit ec: ExecutionContext): Future[Seq[Channel]] = {
    val res = makeApiRequest("channels.list")
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

case class HistoryChunk (
  latest: Long,
  messages: Seq[JsValue],
  has_more: Boolean
) // TODO: Message

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