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

  def test()(implicit ec: ExecutionContext): Future[Boolean] = {
    val res = makeApiRequest("api.test")
    extract[Boolean](res, "ok")
  }

  def testAuth()(implicit ec: ExecutionContext): Future[AuthIdentity] = {
    val res = makeApiRequest("auth.test")
    res.map(_.as[AuthIdentity])
  }

  def listChannels()(implicit ec: ExecutionContext): Future[Seq[Channel]] = {
    val res = makeApiRequest("channels.list")
    extract[Seq[Channel]](res, "channels")
  }

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
    var params = Seq[(String,String)](("channel" -> channelId))
    latest.foreach(l => params :+= ("latest" -> l.toString))
    oldest.foreach(o => params :+= ("oldest" -> o.toString))
    inclusive.foreach(i => params :+= ("inclusive" -> i.toString))
    count.foreach(c => params :+= ("count" -> c.toString))
    val res = makeApiRequest("channels.history", params: _*)
    res.map(_.as[HistoryChunk])
  }

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
}

case class ApiError(code: String) extends Exception(code)
case class HistoryChunk(latest: Long, messages: Seq[JsValue], has_more: Boolean) // TODO: Message