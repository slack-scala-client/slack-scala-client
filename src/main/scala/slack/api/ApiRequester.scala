package slack.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{ContentTypes, HttpMethods, HttpRequest, MessageEntity, Uri}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import play.api.libs.json.{Format, JsValue, Json}
import slack.models.{ResponseMetadata, RetryAfter, SlackFile}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

class ApiRequester(
  token: String,
  slackApiBaseUri: Uri,
  maybeSettings: Option[ConnectionPoolSettings],
  toStrictTimeout: FiniteDuration,
  retries: Int,
  maxBackoff: FiniteDuration
) {
  import concurrent.duration._

  private val apiBaseRequest = HttpRequest(uri = slackApiBaseUri)

  private val apiBaseWithTokenRequest = apiBaseRequest.withUri(
    apiBaseRequest.uri.withQuery(Uri.Query((apiBaseRequest.uri.query() :+ ("token" -> token)): _*))
  )

  def makeApiRequest(request: HttpRequest)(implicit system: ActorSystem): Future[Either[RetryAfter, JsValue]] = {
    implicit val mat = ActorMaterializer()
    implicit val ec  = system.dispatcher
    val connectionPoolSettings: ConnectionPoolSettings = maybeSettings.getOrElse(ConnectionPoolSettings(system))
    Http().singleRequest(request, settings = connectionPoolSettings).flatMap {
      case response if response.status.intValue == 200 =>
        response.entity.toStrict(toStrictTimeout).map { entity =>
          val parsed = Json.parse(entity.data.decodeString("UTF-8"))
          if ((parsed \ "ok").as[Boolean]) {
            Right(parsed)
          } else {
            throw ApiError((parsed \ "error").as[String])
          }
        }
      case response if response.status.intValue == 429 =>
        response.entity.discardBytes()
        val retryAfter = RetryAfter.responseToInt(response)
        Future.successful(Left(RetryAfter(retryAfter)))
      case response =>
        response.entity.toStrict(toStrictTimeout).map { entity =>
          throw InvalidResponseError(response.status.intValue, entity.data.decodeString("UTF-8"))
        }
    }
  }

  def uploadFileFromEntity(entity: MessageEntity,
    filetype: Option[String],
    filename: Option[String],
    title: Option[String],
    initialComment: Option[String],
    channels: Option[Seq[String]],
    thread_ts: Option[String])(implicit system: ActorSystem): Future[SlackFile] = {
    val params = Seq(
      "filetype" -> filetype,
      "filename" -> filename,
      "title" -> title,
      "initial_comment" -> initialComment,
      "channels" -> channels.map(_.mkString(",")),
      "thread_ts" -> thread_ts
    )
    val request =
      addSegment(apiBaseWithTokenRequest, "files.upload").withEntity(entity).withMethod(method = HttpMethods.POST)
    makeApiRequest(addQueryParams(request, cleanParams(params))).flatMap {
      case Right(res) =>
        Future.successful((res \ "file").as[SlackFile])
      case Left(retryAfter) =>
        throw retryAfter.invalidResponseError
    }(system.dispatcher)
  }

  def makeApiMethodRequest(apiMethod: String,
    queryParams: (String, Any)*)(implicit system: ActorSystem): Future[JsValue] = {
    val req = addSegment(apiBaseWithTokenRequest, apiMethod)
    makeApiRequest(addQueryParams(req, cleanParams(queryParams))).map {
      case Right(jsValue) =>
        jsValue
      case Left(retryAfter) =>
        throw retryAfter.invalidResponseError
    }(system.dispatcher)
  }

  def makeApiMethodRequestWithRetryAfter(apiMethod: String,
    queryParams: (String, Any)*)(implicit system: ActorSystem): Future[Either[RetryAfter, JsValue]] = {
    val req = addSegment(apiBaseWithTokenRequest, apiMethod)
    makeApiRequest(addQueryParams(req, cleanParams(queryParams)))
  }

  def paginateCollection[T](apiMethod: String,
    queryParams: Seq[(String, Any)],
    field: String,
    initialResults: Seq[T] = Seq.empty[T])(implicit system: ActorSystem, fmt: Format[Seq[T]]): Future[Seq[T]] = {
    implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))
    implicit val ec: ExecutionContext = system.dispatcher

    RestartSource.onFailuresWithBackoff(2.seconds, maxBackoff, 0.2, retries)(() => {
      Source.fromFuture(
        makeApiMethodRequestWithRetryAfter(apiMethod, queryParams:_*).flatMap {
          case Right(jsValue) =>
            Future.successful(jsValue)
          case Left(retryAfter) =>
            akka.pattern.after(retryAfter.finiteDuration, system.scheduler) {
              Future.failed(retryAfter.invalidResponseError)
            }
        }
      )
    }).runWith(Sink.head).flatMap { res =>
      val nextResults = (res \ field).as[Seq[T]] ++ initialResults
      (res \ "response_metadata").asOpt[ResponseMetadata].flatMap { metadata =>
        metadata.next_cursor.filter(_.nonEmpty)
      } match {
        case Some(nextCursor) =>
          val newParams = queryParams.toMap + ("cursor" -> nextCursor)
          paginateCollection(
            apiMethod = apiMethod,
            queryParams = newParams.toSeq,
            field = field,
            initialResults = nextResults)
        case None =>
          Future.successful(nextResults)
      }
    }
  }
  def makeApiJsonRequest(apiMethod: String, json: JsValue)(implicit system: ActorSystem): Future[JsValue] = {
    val req = addSegment(apiBaseRequest, apiMethod)
      .withMethod(HttpMethods.POST)
      .withHeaders(Authorization(OAuth2BearerToken(token)))
      .withEntity(ContentTypes.`application/json`, json.toString())
    makeApiRequest(req).map {
      case Right(jsValue) =>
        jsValue
      case Left(retryAfter) =>
        throw retryAfter.invalidResponseError
    }(system.dispatcher)
  }

  private[api] def addSegment(request: HttpRequest, segment: String): HttpRequest = {
    request.withUri(request.uri.withPath(request.uri.path + segment))
  }

  private[api] def addQueryParams(request: HttpRequest, queryParams: Seq[(String, String)]): HttpRequest = {
    request.withUri(request.uri.withQuery(Uri.Query((request.uri.query() ++ queryParams): _*)))
  }

  private[api] def cleanParams(params: Seq[(String, Any)]): Seq[(String, String)] = {
    var paramList = Seq.empty[(String, String)]
    params.foreach {
      case (k, Some(v)) => paramList :+= (k -> v.toString)
      case (k, None)    => // Nothing - Filter out none
      case (k, v)       => paramList :+= (k -> v.toString)
    }
    paramList
  }

}
