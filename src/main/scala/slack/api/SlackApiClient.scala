package slack.api

import slack.models._

import scala.concurrent.ExecutionContext

import dispatch._
import play.api.libs.json._

object SlackApiClient {
    def apply(token: String): SlackApiClient = {
        new SlackApiClient(token)
    }
}

class SlackApiClient(token: String) {

  val apiBase = url("https://slack.com/api").addQueryParameter("token", token)

  def listChannels()(implicit ec: ExecutionContext): Future[Seq[Channel]] = {
    val res = makeApiRequest(apiBase / "channels.list")
    extract[Seq[Channel]](res, "channels")
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