package slack.api

import java.io.File
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.settings.{
  ClientConnectionSettings,
  ConnectionPoolSettings
}
import akka.http.scaladsl.{ClientTransport, Http}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import com.typesafe.config.ConfigFactory
import play.api.libs.json._
import slack.models._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

object SlackApiClient {

  private[this] val config = ConfigFactory.load()
  private[this] val useProxy: Boolean =
    config.getBoolean("slack-scala-client.http.useproxy")

  private[this] val maybeSettings: Option[ConnectionPoolSettings] =
    if (useProxy) {
      val proxyHost = config.getString("slack-scala-client.http.proxyHost")
      val proxyPort =
        config.getString("slack-scala-client.http.proxyPort").toInt

      val httpsProxyTransport = ClientTransport.httpsProxy(
        InetSocketAddress.createUnresolved(proxyHost, proxyPort)
      )

      Some(
        ConnectionPoolSettings(config)
          .withConnectionSettings(
            ClientConnectionSettings(config)
              .withTransport(httpsProxyTransport)
          )
      )
    } else {
      None
    }

  private[this] val toStrictTimeout =
    config.getInt("api.tostrict.timeout").seconds

  private[api] val retries = config.getInt("api.retries")
  private[api] val maxBackoff = config.getInt("api.maxBackoff").seconds

  private[api] implicit val rtmStartStateFmt = Json.format[RtmStartState]
  private[api] implicit val accessTokenFmt = Json.format[AccessToken]
  private[api] implicit val historyChunkFmt = Json.format[HistoryChunk]
  private[api] implicit val repliesChunkFmt = Json.format[RepliesChunk]
  private[api] implicit val pagingObjectFmt = Json.format[PagingObject]
  private[api] implicit val filesResponseFmt = Json.format[FilesResponse]
  private[api] implicit val fileInfoFmt = Json.format[FileInfo]
  private[api] implicit val reactionsResponseFmt =
    Json.format[ReactionsResponse]

  val defaultSlackApiBaseUri = Uri("https://slack.com/api/")

  def apply(
      token: String,
      slackApiBaseUri: Uri = defaultSlackApiBaseUri
  ): SlackApiClient = {
    new SlackApiClient(token, slackApiBaseUri)
  }

  def exchangeOauthForToken(
      clientId: String,
      clientSecret: String,
      code: String,
      redirectUri: Option[String] = None,
      slackApiBaseUri: Uri = defaultSlackApiBaseUri
  )(implicit system: ActorSystem): Future[AccessToken] = {
    val params =
      Seq(
        "client_id" -> clientId,
        "client_secret" -> clientSecret,
        "code" -> code,
        "redirect_uri" -> redirectUri
      )
    makeApiRequest(
      addQueryParams(
        addSegment(HttpRequest(uri = slackApiBaseUri), "oauth.access"),
        cleanParams(params)
      )
    ).map {
      case Right(jsValue) =>
        jsValue.as[AccessToken]
      case Left(retryAfter) =>
        throw retryAfter.invalidResponseError
    }(system.dispatcher)
  }

  private def makeApiRequest(
      request: HttpRequest
  )(implicit system: ActorSystem): Future[Either[RetryAfter, JsValue]] = {
    implicit val mat = ActorMaterializer()
    implicit val ec = system.dispatcher
    val connectionPoolSettings: ConnectionPoolSettings =
      maybeSettings.getOrElse(ConnectionPoolSettings(system))
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
          throw InvalidResponseError(
            response.status.intValue,
            entity.data.decodeString("UTF-8")
          )
        }
    }
  }

  private def extract[T](jsFuture: Future[JsValue], field: String)(implicit
      system: ActorSystem,
      fmt: Format[T]
  ): Future[T] = {
    jsFuture.map(js => (js \ field).as[T])(system.dispatcher)
  }

  private def addQueryParams(
      request: HttpRequest,
      queryParams: Seq[(String, String)]
  ): HttpRequest = {
    request.withUri(
      request.uri.withQuery(Uri.Query((request.uri.query() ++ queryParams): _*))
    )
  }

  private def cleanParams(params: Seq[(String, Any)]): Seq[(String, String)] = {
    var paramList = Seq.empty[(String, String)]
    params.foreach {
      case (k, Some(v)) => paramList :+= (k -> v.toString)
      case (k, None)    => // Nothing - Filter out none
      case (k, v)       => paramList :+= (k -> v.toString)
    }
    paramList
  }

  private def addSegment(request: HttpRequest, segment: String): HttpRequest = {
    request.withUri(request.uri.withPath(request.uri.path + segment))
  }

  case class SlackFileMetaData(
      id: Option[String],
      created: Option[Long],
      timestamp: Option[Long],
      name: Option[String],
      mimeType: Option[String],
      fileType: Option[String],
      user: Option[String],
      editable: Option[Boolean],
      size: Option[Int],
      mode: Option[String],
      isExternal: Option[Boolean],
      externalType: Option[String],
      isPublic: Option[Boolean],
      publicUrlShared: Option[Boolean],
      displayAsBot: Option[Boolean],
      urlPrivate: Option[String],
      urlPrivateDownload: Option[String],
      permalink: Option[String],
      permalinkPublic: Option[String],
      preview: Option[String],
      previewHighlight: Option[String],
      lines: Option[Int],
      commentsCount: Option[Int],
      isStarred: Option[Boolean],
      channels: Option[Seq[String]],
      ims: Option[Seq[String]],
      hasRichPreview: Option[Boolean]
  )
  case class DetailedFileInfo(
      ok: Option[Boolean],
      file: SlackFileMetaData,
      content: Option[String],
      isTruncated: Option[Boolean],
      comments: Option[Seq[String]]
  )

  implicit val SlackFileMetaDataReader: Reads[SlackFileMetaData] =
    new Reads[SlackFileMetaData] {
      override def reads(json: JsValue): JsResult[SlackFileMetaData] = {
        for {
          id <- (json \ "id").validateOpt[String]
          created <- (json \ "created").validateOpt[Long]
          timestamp <- (json \ "timestamp").validateOpt[Long]
          name <- (json \ "name").validateOpt[String]
          mimeType <- (json \ "mimetype").validateOpt[String]
          fileType <- (json \ "filetype").validateOpt[String]
          user <- (json \ "user").validateOpt[String]
          editable <- (json \ "editable").validateOpt[Boolean]
          size <- (json \ "size").validateOpt[Int]
          mode <- (json \ "mode").validateOpt[String]
          isExternal <- (json \ "is_external").validateOpt[Boolean]
          externalType <- (json \ "external_type").validateOpt[String]
          isPublic <- (json \ "is_public").validateOpt[Boolean]
          publicUrlShared <- (json \ "public_url_shared").validateOpt[Boolean]
          displayAsBot <- (json \ "display_as_bot").validateOpt[Boolean]
          username <- (json \ "username").validateOpt[String]
          urlPrivate <- (json \ "url_private").validateOpt[String]
          permalink <- (json \ "permalink").validateOpt[String]
          permalinkPublic <- (json \ "permalink_public").validateOpt[String]
          preview <- (json \ "preview").validateOpt[String]
          previewHighlight <- (json \ "preview_highlight").validateOpt[String]
          lines <- (json \ "lines").validateOpt[Int]
          commentsCount <- (json \ "comments_count").validateOpt[Int]
          isStarred <- (json \ "is_starred").validateOpt[Boolean]
          channels <- (json \ "channels").validateOpt[Seq[String]]
          ims <- (json \ "ims").validateOpt[Seq[String]]
          hasRichPreview <- (json \ "has_rich_preview").validateOpt[Boolean]
        } yield SlackFileMetaData(
          id,
          created,
          timestamp,
          name,
          mimeType,
          fileType,
          user,
          editable,
          size,
          mode,
          isExternal,
          externalType,
          isPublic,
          publicUrlShared,
          displayAsBot,
          username,
          urlPrivate,
          permalink,
          permalinkPublic,
          preview,
          previewHighlight,
          lines,
          commentsCount,
          isStarred,
          channels,
          ims,
          hasRichPreview
        )
      }
    }
  private implicit val DetailedFileInfoReader: Reads[DetailedFileInfo] =
    new Reads[DetailedFileInfo] {
      override def reads(json: JsValue): JsResult[DetailedFileInfo] = {
        for {
          ok <- (json \ "ok").validateOpt[Boolean]
          fileMetaData <- (json \ "file").validate[SlackFileMetaData]
          content <- (json \ "content").validateOpt[String]
          isTruncated <- (json \ "is_truncated").validateOpt[Boolean]
          comments <- (json \ "comments").validateOpt[Seq[String]]
        } yield DetailedFileInfo(
          ok,
          fileMetaData,
          content,
          isTruncated,
          comments
        )
      }
    }
}

import slack.api.SlackApiClient._

class SlackApiClient private (token: String, slackApiBaseUri: Uri) {

  private val apiBaseRequest = HttpRequest(uri = slackApiBaseUri)

  private val apiBaseWithTokenRequest =
    apiBaseRequest.withHeaders(Authorization(OAuth2BearerToken(token)))

  /** ***********************
    */
  /** *   Test Endpoints   **
    */
  /** ***********************
    */
  def test()(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("api.test")
    extract[Boolean](res, "ok")
  }

  def testAuth()(implicit system: ActorSystem): Future[AuthIdentity] = {
    val res = makeApiMethodRequest("auth.test")
    res.map(_.as[AuthIdentity])(system.dispatcher)
  }

  /** ************************
    */
  /** *  Channel Endpoints  **
    */
  /** ************************
    */
  def archiveChannel(
      channelId: String
  )(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("channels.archive", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def createChannel(
      name: String
  )(implicit system: ActorSystem): Future[Channel] = {
    val res = makeApiMethodRequest("channels.create", "name" -> name)
    extract[Channel](res, "channel")
  }

  def getChannelHistory(
      channelId: String,
      latest: Option[String] = None,
      oldest: Option[String] = None,
      inclusive: Option[Int] = None,
      count: Option[Int] = None
  )(implicit system: ActorSystem): Future[HistoryChunk] = {
    val res = makeApiMethodRequest(
      "channels.history",
      "channel" -> channelId,
      "latest" -> latest,
      "oldest" -> oldest,
      "inclusive" -> inclusive,
      "count" -> count
    )
    res.map(_.as[HistoryChunk])(system.dispatcher)
  }

  def getChannelInfo(
      channelId: String
  )(implicit system: ActorSystem): Future[Channel] = {
    val res = makeApiMethodRequest("channels.info", "channel" -> channelId)
    extract[Channel](res, "channel")
  }

  def inviteToChannel(channelId: String, userId: String)(implicit
      system: ActorSystem
  ): Future[Channel] = {
    val res = makeApiMethodRequest(
      "channels.invite",
      "channel" -> channelId,
      "user" -> userId
    )
    extract[Channel](res, "channel")
  }

  def joinChannel(
      channelId: String
  )(implicit system: ActorSystem): Future[Channel] = {
    val res = makeApiMethodRequest("channels.join", "channel" -> channelId)
    extract[Channel](res, "channel")
  }

  def kickFromChannel(channelId: String, userId: String)(implicit
      system: ActorSystem
  ): Future[Boolean] = {
    val res = makeApiMethodRequest(
      "channels.kick",
      "channel" -> channelId,
      "user" -> userId
    )
    extract[Boolean](res, "ok")
  }

  @deprecated("use listConversations", "0.2.18")
  def listChannels(
      excludeArchived: Boolean = false
  )(implicit system: ActorSystem): Future[Seq[Channel]] = {
    listConversations(
      Seq(PublicChannel, PrivateChannel),
      if (excludeArchived) 1 else 0
    )
  }

  def listConversations(
      channelTypes: Seq[ConversationType] = Seq(PublicChannel),
      excludeArchived: Int = 0
  )(implicit system: ActorSystem): Future[Seq[Channel]] = {
    val params = Seq(
      "exclude_archived" -> excludeArchived.toString,
      "types" -> channelTypes.map(_.conversationType).mkString(",")
    )
    paginateCollection[Channel](
      apiMethod = "conversations.list",
      queryParams = params,
      field = "channels"
    )
  }

  def getConversationInfo(
      channelId: String,
      includeLocale: Boolean = true,
      includeNumMembers: Boolean = false
  )(implicit system: ActorSystem): Future[Channel] = {
    val res = makeApiMethodRequest(
      "conversations.info",
      "channel" -> channelId,
      "include_locale" -> includeLocale.toString,
      "include_num_members" -> includeNumMembers.toString
    )
    extract[Channel](res, "channel")
  }

  def setConversationTopic(channelId: String, topic: String)(implicit
      system: ActorSystem
  ): Future[Boolean] = {
    val res = makeApiMethodRequest(
      "conversations.setTopic",
      "channel" -> channelId,
      "topic" -> topic
    )
    extract[Boolean](res, "ok")
  }

  def leaveChannel(
      channelId: String
  )(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("channels.leave", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def markChannel(channelId: String, ts: String)(implicit
      system: ActorSystem
  ): Future[Boolean] = {
    val res =
      makeApiMethodRequest("channels.mark", "channel" -> channelId, "ts" -> ts)
    extract[Boolean](res, "ok")
  }

  // TODO: Lite Channel Object
  def renameChannel(channelId: String, name: String)(implicit
      system: ActorSystem
  ): Future[Boolean] = {
    val res = makeApiMethodRequest(
      "channels.rename",
      "channel" -> channelId,
      "name" -> name
    )
    extract[Boolean](res, "ok")
  }

  def getChannelReplies(channelId: String, thread_ts: String)(implicit
      system: ActorSystem
  ): Future[RepliesChunk] = {
    val res = makeApiMethodRequest(
      "channels.replies",
      "channel" -> channelId,
      "thread_ts" -> thread_ts
    )
    res.map(_.as[RepliesChunk])(system.dispatcher)
  }

  def setChannelPurpose(channelId: String, purpose: String)(implicit
      system: ActorSystem
  ): Future[String] = {
    val res = makeApiMethodRequest(
      "channels.setPurpose",
      "channel" -> channelId,
      "purpose" -> purpose
    )
    extract[String](res, "purpose")
  }

  def setChannelTopic(channelId: String, topic: String)(implicit
      system: ActorSystem
  ): Future[String] = {
    val res = makeApiMethodRequest(
      "channels.setTopic",
      "channel" -> channelId,
      "topic" -> topic
    )
    extract[String](res, "topic")
  }

  def unarchiveChannel(
      channelId: String
  )(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("channels.unarchive", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  /** ***********************
    */
  /** **  Chat Endpoints  ***
    */
  /** ***********************
    */
  def deleteChat(channelId: String, ts: String, asUser: Option[Boolean] = None)(
      implicit system: ActorSystem
  ): Future[Boolean] = {
    val params = Seq("channel" -> channelId, "ts" -> ts)
    val res = makeApiMethodRequest(
      "chat.delete",
      asUser.map(b => params :+ ("as_user" -> b)).getOrElse(params): _*
    )
    extract[Boolean](res, "ok")
  }

  def postChatEphemeral(
      channelId: String,
      text: String,
      user: String,
      asUser: Option[Boolean] = None,
      parse: Option[String] = None,
      attachments: Option[Seq[Attachment]] = None,
      blocks: Option[Seq[Block]] = None,
      linkNames: Option[Boolean] = None
  )(implicit system: ActorSystem): Future[String] = {
    val json =
      Json.obj("channel" -> channelId, "text" -> text, "user" -> user) ++
        JsObject(
          Seq(
            asUser.map("as_user" -> Json.toJson(_)),
            parse.map("parse" -> Json.toJson(_)),
            linkNames.map("link_names" -> Json.toJson(_)),
            attachments.map("attachments" -> Json.toJson(_)),
            blocks.map("blocks" -> Json.toJson(_))
          ).flatten
        )
    val res = makeApiJsonRequest("chat.postEphemeral", json)
    extract[String](res, "message_ts")
  }

  def postChatMessage(
      channelId: String,
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
      replyBroadcast: Option[Boolean] = None
  )(implicit system: ActorSystem): Future[String] = {
    val json = Json.obj("channel" -> channelId, "text" -> text) ++
      JsObject(
        Seq(
          username.map("username" -> Json.toJson(_)),
          asUser.map("as_user" -> Json.toJson(_)),
          parse.map("parse" -> Json.toJson(_)),
          linkNames.map("link_names" -> Json.toJson(_)),
          attachments.map("attachments" -> Json.toJson(_)),
          blocks.map("blocks" -> Json.toJson(_)),
          unfurlLinks.map("unfurl_links" -> Json.toJson(_)),
          unfurlMedia.map("unfurl_media" -> Json.toJson(_)),
          iconUrl.map("icon_url" -> Json.toJson(_)),
          iconEmoji.map("icon_emoji" -> Json.toJson(_)),
          replaceOriginal.map("replace_original" -> Json.toJson(_)),
          deleteOriginal.map("delete_original" -> Json.toJson(_)),
          threadTs.map("thread_ts" -> Json.toJson(_)),
          replyBroadcast.map("reply_broadcast" -> Json.toJson(_))
        ).flatten
      )
    val res = makeApiJsonRequest("chat.postMessage", json)
    extract[String](res, "ts")
  }

  def updateChatMessage(
      channelId: String,
      ts: String,
      text: String,
      attachments: Option[Seq[Attachment]] = None,
      blocks: Option[Seq[Block]] = None,
      parse: Option[String] = None,
      linkNames: Option[String] = None,
      asUser: Option[Boolean] = None,
      threadTs: Option[String] = None
  )(implicit system: ActorSystem): Future[UpdateResponse] = {
    val json = Json.obj("channel" -> channelId, "ts" -> ts, "text" -> text) ++
      JsObject(
        Seq(
          attachments.map("attachments" -> Json.toJson(_)),
          blocks.map("blocks" -> Json.toJson(_)),
          parse.map("parse" -> Json.toJson(_)),
          linkNames.map("link_names" -> Json.toJson(_)),
          asUser.map("as_user" -> Json.toJson(_)),
          threadTs.map("thread_ts" -> Json.toJson(_))
        ).flatten
      )
    val res = makeApiJsonRequest("chat.update", json)
    res.map(_.as[UpdateResponse])(system.dispatcher)
  }

  /** *************************
    */
  /** **  Dialog Endpoints  ***
    */
  /** *************************
    */
  def openDialog(triggerId: String, dialog: Dialog)(implicit
      system: ActorSystem
  ): Future[Boolean] = {
    val res =
      makeApiJsonRequest(
        "dialog.open",
        Json.obj(
          "trigger_id" -> triggerId,
          "dialog" -> Json.toJson(dialog).toString()
        )
      )
    extract[Boolean](res, "ok")
  }

  /** ************************
    */
  /** **  Emoji Endpoints  ***
    */
  /** ************************
    */
  def listEmojis()(implicit
      system: ActorSystem
  ): Future[Map[String, String]] = {
    val res = makeApiMethodRequest("emoji.list")
    extract[Map[String, String]](res, "emoji")
  }

  /** ***********************
    */
  /** **  File Endpoints  ***
    */
  /** ***********************
    */
  def deleteFile(
      fileId: String
  )(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("files.delete", "file" -> fileId)
    extract[Boolean](res, "ok")
  }

  def getFileInfo(
      fileId: String,
      count: Option[Int] = None,
      page: Option[Int] = None
  )(implicit
      system: ActorSystem
  ): Future[FileInfo] = {
    val res = makeApiMethodRequest(
      "files.info",
      "file" -> fileId,
      "count" -> count,
      "page" -> page
    )
    res.map(_.as[FileInfo])(system.dispatcher)
  }

  def getDetailedFileInfo(
      file_id: String,
      count: Option[Int] = None,
      page: Option[Int] = None
  )(implicit system: ActorSystem): Future[DetailedFileInfo] = {
    makeApiMethodRequest(
      "files.info",
      "file" -> file_id,
      "count" -> count,
      "page" -> page
    )
      .map(_.validate[DetailedFileInfo].get)(system.dispatcher)
  }

  def listFiles(
      userId: Option[String] = None,
      tsFrom: Option[String] = None,
      tsTo: Option[String] = None,
      types: Option[Seq[String]] = None,
      count: Option[Int] = None,
      page: Option[Int] = None
  )(implicit system: ActorSystem): Future[FilesResponse] = {
    val res = makeApiMethodRequest(
      "files.list",
      "user" -> userId,
      "ts_from" -> tsFrom,
      "ts_to" -> tsTo,
      "types" -> types.map(_.mkString(",")),
      "count" -> count,
      "page" -> page
    )
    res.map(_.as[FilesResponse])(system.dispatcher)
  }

  def uploadFile(
      content: Either[File, Array[Byte]],
      filetype: Option[String] = None,
      filename: Option[String] = None,
      title: Option[String] = None,
      initialComment: Option[String] = None,
      channels: Option[Seq[String]] = None,
      thread_ts: Option[String] = None
  )(implicit system: ActorSystem): Future[SlackFile] = {
    val entity = content match {
      case Right(bytes) => createEntity(filename.getOrElse("file"), bytes)
      case Left(file)   => createEntity(file)
    }
    uploadFileFromEntity(
      entity,
      filetype,
      filename,
      title,
      initialComment,
      channels,
      thread_ts
    )
  }

  /** ************************
    */
  /** **  Group Endpoints  ***
    */
  /** ************************
    */
  def archiveGroup(
      channelId: String
  )(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("groups.archive", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def closeGroup(
      channelId: String
  )(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("groups.close", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def createGroup(name: String)(implicit system: ActorSystem): Future[Group] = {
    val res = makeApiMethodRequest("groups.create", "name" -> name)
    extract[Group](res, "group")
  }

  def createChildGroup(
      channelId: String
  )(implicit system: ActorSystem): Future[Group] = {
    val res = makeApiMethodRequest("groups.createChild", "channel" -> channelId)
    extract[Group](res, "group")
  }

  def getGroupHistory(
      channelId: String,
      latest: Option[String] = None,
      oldest: Option[String] = None,
      inclusive: Option[Int] = None,
      count: Option[Int] = None
  )(implicit system: ActorSystem): Future[HistoryChunk] = {
    val res = makeApiMethodRequest(
      "groups.history",
      "channel" -> channelId,
      "latest" -> latest,
      "oldest" -> oldest,
      "inclusive" -> inclusive,
      "count" -> count
    )
    res.map(_.as[HistoryChunk])(system.dispatcher)
  }

  def getGroupInfo(
      channelId: String
  )(implicit system: ActorSystem): Future[Group] = {
    val res = makeApiMethodRequest("groups.info", "channel" -> channelId)
    extract[Group](res, "group")
  }

  def inviteToGroup(channelId: String, userId: String)(implicit
      system: ActorSystem
  ): Future[Group] = {
    val res = makeApiMethodRequest(
      "groups.invite",
      "channel" -> channelId,
      "user" -> userId
    )
    extract[Group](res, "group")
  }

  def kickFromGroup(channelId: String, userId: String)(implicit
      system: ActorSystem
  ): Future[Boolean] = {
    val res = makeApiMethodRequest(
      "groups.kick",
      "channel" -> channelId,
      "user" -> userId
    )
    extract[Boolean](res, "ok")
  }

  def leaveGroup(
      channelId: String
  )(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("groups.leave", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def listGroups(
      excludeArchived: Int = 0
  )(implicit system: ActorSystem): Future[Seq[Group]] = {
    val res = makeApiMethodRequest(
      "groups.list",
      "exclude_archived" -> excludeArchived.toString
    )
    extract[Seq[Group]](res, "groups")
  }

  def markGroup(channelId: String, ts: String)(implicit
      system: ActorSystem
  ): Future[Boolean] = {
    val res =
      makeApiMethodRequest("groups.mark", "channel" -> channelId, "ts" -> ts)
    extract[Boolean](res, "ok")
  }

  def openGroup(
      channelId: String
  )(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("groups.open", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  // TODO: Lite Group Object
  def renameGroup(channelId: String, name: String)(implicit
      system: ActorSystem
  ): Future[Boolean] = {
    val res = makeApiMethodRequest(
      "groups.rename",
      "channel" -> channelId,
      "name" -> name
    )
    extract[Boolean](res, "ok")
  }

  def setGroupPurpose(channelId: String, purpose: String)(implicit
      system: ActorSystem
  ): Future[String] = {
    val res = makeApiMethodRequest(
      "groups.setPurpose",
      "channel" -> channelId,
      "purpose" -> purpose
    )
    extract[String](res, "purpose")
  }

  def setGroupTopic(channelId: String, topic: String)(implicit
      system: ActorSystem
  ): Future[String] = {
    val res = makeApiMethodRequest(
      "groups.setTopic",
      "channel" -> channelId,
      "topic" -> topic
    )
    extract[String](res, "topic")
  }

  def unarchiveGroup(
      channelId: String
  )(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("groups.unarchive", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  /** *********************
    */
  /** **  IM Endpoints  ***
    */
  /** *********************
    */
  def closeIm(
      channelId: String
  )(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("im.close", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def getImHistory(
      channelId: String,
      latest: Option[String] = None,
      oldest: Option[String] = None,
      inclusive: Option[Int] = None,
      count: Option[Int] = None
  )(implicit system: ActorSystem): Future[HistoryChunk] = {
    val res = makeApiMethodRequest(
      "im.history",
      "channel" -> channelId,
      "latest" -> latest,
      "oldest" -> oldest,
      "inclusive" -> inclusive,
      "count" -> count
    )
    res.map(_.as[HistoryChunk])(system.dispatcher)
  }

  def listIms()(implicit system: ActorSystem): Future[Seq[Im]] = {
    val res = makeApiMethodRequest("im.list")
    extract[Seq[Im]](res, "ims")
  }

  def markIm(channelId: String, ts: String)(implicit
      system: ActorSystem
  ): Future[Boolean] = {
    val res =
      makeApiMethodRequest("im.mark", "channel" -> channelId, "ts" -> ts)
    extract[Boolean](res, "ok")
  }

  def openIm(userId: String)(implicit system: ActorSystem): Future[String] = {
    val res = makeApiMethodRequest("im.open", "user" -> userId)
    res.map(r => (r \ "channel" \ "id").as[String])(system.dispatcher)
  }

  /** ***********************
    */
  /** **  MPIM Endpoints  ***
    */
  /** ***********************
    */
  def openMpim(
      userIds: Seq[String]
  )(implicit system: ActorSystem): Future[String] = {
    val res =
      makeApiMethodRequest("mpim.open", "users" -> userIds.mkString(","))
    res.map(r => (r \ "group" \ "id").as[String])(system.dispatcher)
  }

  def closeMpim(
      channelId: String
  )(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("mpim.close", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def listMpims()(implicit system: ActorSystem): Future[Seq[Group]] = {
    val res = makeApiMethodRequest("mpim.list")
    extract[Seq[Group]](res, "groups")
  }

  def markMpim(channelId: String, ts: String)(implicit
      system: ActorSystem
  ): Future[Boolean] = {
    val res =
      makeApiMethodRequest("mpim.mark", "channel" -> channelId, "ts" -> ts)
    extract[Boolean](res, "ok")
  }

  def getMpimHistory(
      channelId: String,
      latest: Option[String] = None,
      oldest: Option[String] = None,
      inclusive: Option[Int] = None,
      count: Option[Int] = None
  )(implicit system: ActorSystem): Future[HistoryChunk] = {
    val res = makeApiMethodRequest(
      "mpim.history",
      "channel" -> channelId,
      "latest" -> latest,
      "oldest" -> oldest,
      "inclusive" -> inclusive,
      "count" -> count
    )
    res.map(_.as[HistoryChunk])(system.dispatcher)
  }

  /** ***************************
    */
  /** **  Reaction Endpoints  ***
    */
  /** ***************************
    */
  def addReaction(
      emojiName: String,
      file: Option[String] = None,
      fileComment: Option[String] = None,
      channelId: Option[String] = None,
      timestamp: Option[String] = None
  )(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest(
      "reactions.add",
      "name" -> emojiName,
      "file" -> file,
      "file_comment" -> fileComment,
      "channel" -> channelId,
      "timestamp" -> timestamp
    )
    extract[Boolean](res, "ok")
  }

  def addReactionToMessage(
      emojiName: String,
      channelId: String,
      timestamp: String
  )(implicit
      system: ActorSystem
  ): Future[Boolean] = {
    addReaction(
      emojiName = emojiName,
      channelId = Some(channelId),
      timestamp = Some(timestamp)
    )
  }

  def getReactions(
      file: Option[String] = None,
      fileComment: Option[String] = None,
      channelId: Option[String] = None,
      timestamp: Option[String] = None,
      full: Option[Boolean] = None
  )(implicit system: ActorSystem): Future[Seq[Reaction]] = {
    val res = makeApiMethodRequest(
      "reactions.get",
      "file" -> file,
      "file_comment" -> fileComment,
      "channel" -> channelId,
      "timestamp" -> timestamp,
      "full" -> full
    )
    res.map(r =>
      (r \\ "reactions").headOption
        .map(_.as[Seq[Reaction]])
        .getOrElse(Seq.empty[Reaction])
    )(system.dispatcher)
  }

  def getReactionsForMessage(
      channelId: String,
      timestamp: String,
      full: Option[Boolean] = None
  )(implicit
      system: ActorSystem
  ): Future[Seq[Reaction]] = {
    getReactions(
      channelId = Some(channelId),
      timestamp = Some(timestamp),
      full = full
    )
  }

  def listReactionsForUser(
      userId: Option[String],
      full: Boolean = false,
      count: Option[Int] = None,
      page: Option[Int] = None
  )(implicit system: ActorSystem): Future[ReactionsResponse] = {
    val res = makeApiMethodRequest(
      "reations.list",
      "user" -> userId,
      "full" -> full,
      "count" -> count,
      "page" -> page
    )
    res.map(_.as[ReactionsResponse])(system.dispatcher)
  }

  def removeReaction(
      emojiName: String,
      file: Option[String] = None,
      fileComment: Option[String] = None,
      channelId: Option[String] = None,
      timestamp: Option[String] = None
  )(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest(
      "reactions.remove",
      "name" -> emojiName,
      "file" -> file,
      "file_comment" -> fileComment,
      "channel" -> channelId,
      "timestamp" -> timestamp
    )
    extract[Boolean](res, "ok")
  }

  def removeReactionFromMessage(
      emojiName: String,
      channelId: String,
      timestamp: String
  )(implicit
      system: ActorSystem
  ): Future[Boolean] = {
    removeReaction(
      emojiName = emojiName,
      channelId = Some(channelId),
      timestamp = Some(timestamp)
    )
  }

  /** **********************
    */
  /** **  RTM Endpoints  ***
    */
  /** **********************
    */
  def startRealTimeMessageSession()(implicit
      system: ActorSystem
  ): Future[RtmStartState] = {
    val res = makeApiMethodRequest("rtm.start")
    implicit val ec: ExecutionContext = system.dispatcher
    res.map { value: JsValue =>
      try {
        value.as[RtmStartState]
      } catch {
        case e: Exception =>
          throw new IllegalStateException(
            "Failed to parse the response for rtm.start: " + value.toString,
            e
          )
      }
    }
  }

  /** *************************
    */
  /** **  Search Endpoints  ***
    */
  /** *************************
    */
  // TODO: Return proper search results (not JsValue)
  def searchAll(
      query: String,
      sort: Option[String] = None,
      sortDir: Option[String] = None,
      highlight: Option[String] = None,
      count: Option[Int] = None,
      page: Option[Int] = None
  )(implicit system: ActorSystem): Future[JsValue] = {
    makeApiMethodRequest(
      "search.all",
      "query" -> query,
      "sort" -> sort,
      "sortDir" -> sortDir,
      "highlight" -> highlight,
      "count" -> count,
      "page" -> page
    )
  }

  // TODO: Return proper search results (not JsValue)
  def searchFiles(
      query: String,
      sort: Option[String] = None,
      sortDir: Option[String] = None,
      highlight: Option[String] = None,
      count: Option[Int] = None,
      page: Option[Int] = None
  )(implicit system: ActorSystem): Future[JsValue] = {
    makeApiMethodRequest(
      "search.files",
      "query" -> query,
      "sort" -> sort,
      "sortDir" -> sortDir,
      "highlight" -> highlight,
      "count" -> count,
      "page" -> page
    )
  }

  // TODO: Return proper search results (not JsValue)
  def searchMessages(
      query: String,
      sort: Option[String] = None,
      sortDir: Option[String] = None,
      highlight: Option[String] = None,
      count: Option[Int] = None,
      page: Option[Int] = None
  )(implicit system: ActorSystem): Future[JsValue] = {
    makeApiMethodRequest(
      "search.messages",
      "query" -> query,
      "sort" -> sort,
      "sortDir" -> sortDir,
      "highlight" -> highlight,
      "count" -> count,
      "page" -> page
    )
  }

  /** ************************
    */
  /** **  Stars Endpoints  ***
    */
  /** ************************
    */
  // TODO: Return proper star items (not JsValue)
  def listStars(
      userId: Option[String] = None,
      count: Option[Int] = None,
      page: Option[Int] = None
  )(implicit
      system: ActorSystem
  ): Future[JsValue] = {
    makeApiMethodRequest(
      "start.list",
      "user" -> userId,
      "count" -> count,
      "page" -> page
    )
  }

  /** ***********************
    */
  /** **  Team Endpoints  ***
    */
  /** ***********************
    */
  // TODO: Parse actual result type: https://api.slack.com/methods/team.accessLogs
  def getTeamAccessLogs(count: Option[Int], page: Option[Int])(implicit
      system: ActorSystem
  ): Future[JsValue] = {
    makeApiMethodRequest("team.accessLogs", "count" -> count, "page" -> page)
  }

  // TODO: Parse actual value type: https://api.slack.com/methods/team.info
  def getTeamInfo()(implicit system: ActorSystem): Future[JsValue] = {
    makeApiMethodRequest("team.info")
  }

  /** ***********************
    */
  /** **  User Endpoints  ***
    */
  /** ***********************
    */
  // TODO: Full payload for authed user: https://api.slack.com/methods/users.getPresence
  def getUserPresence(
      userId: String
  )(implicit system: ActorSystem): Future[String] = {
    val res = makeApiMethodRequest("users.getPresence", "user" -> userId)
    extract[String](res, "presence")
  }

  def getUserInfo(
      userId: String
  )(implicit system: ActorSystem): Future[User] = {
    val res = makeApiMethodRequest("users.info", "user" -> userId)
    extract[User](res, "user")
  }

  def listUsers()(implicit system: ActorSystem): Future[Seq[User]] = {
    val params = Seq("limit" -> 100)
    paginateCollection[User](
      apiMethod = "users.list",
      queryParams = params,
      field = "members"
    )
  }

  def setUserActive(
      userId: String
  )(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("users.setActive", "user" -> userId)
    extract[Boolean](res, "ok")
  }

  def setUserPresence(
      presence: String
  )(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("users.setPresence", "presence" -> presence)
    extract[Boolean](res, "ok")
  }

  def lookupUserByEmail(
      emailId: String
  )(implicit system: ActorSystem): Future[User] = {
    val res = makeApiMethodRequest("users.lookupByEmail", "email" -> emailId)
    extract[User](res, "user")
  }

  /** **************************
    */
  /** **  Private Functions  ***
    */
  /** **************************
    */
  private def uploadFileFromEntity(
      entity: MessageEntity,
      filetype: Option[String],
      filename: Option[String],
      title: Option[String],
      initialComment: Option[String],
      channels: Option[Seq[String]],
      thread_ts: Option[String]
  )(implicit system: ActorSystem): Future[SlackFile] = {
    val params = Seq(
      "filetype" -> filetype,
      "filename" -> filename,
      "title" -> title,
      "initial_comment" -> initialComment,
      "channels" -> channels.map(_.mkString(",")),
      "thread_ts" -> thread_ts
    )
    val request =
      addSegment(apiBaseWithTokenRequest, "files.upload")
        .withEntity(entity)
        .withMethod(method = HttpMethods.POST)
    makeApiRequest(addQueryParams(request, cleanParams(params))).flatMap {
      case Right(res) =>
        extract[SlackFile](Future.successful(res), "file")
      case Left(retryAfter) =>
        throw retryAfter.invalidResponseError
    }(system.dispatcher)
  }

  private def makeApiMethodRequest(
      apiMethod: String,
      queryParams: (String, Any)*
  )(implicit system: ActorSystem): Future[JsValue] = {
    val req = addSegment(apiBaseWithTokenRequest, apiMethod)
    makeApiRequest(addQueryParams(req, cleanParams(queryParams))).map {
      case Right(jsValue) =>
        jsValue
      case Left(retryAfter) =>
        throw retryAfter.invalidResponseError
    }(system.dispatcher)
  }

  private def makeApiMethodRequestWithRetryAfter(
      apiMethod: String,
      queryParams: (String, Any)*
  )(implicit system: ActorSystem): Future[Either[RetryAfter, JsValue]] = {
    val req = addSegment(apiBaseWithTokenRequest, apiMethod)
    makeApiRequest(addQueryParams(req, cleanParams(queryParams)))
  }

  private def paginateCollection[T](
      apiMethod: String,
      queryParams: Seq[(String, Any)],
      field: String,
      initialResults: Seq[T] = Seq.empty[T]
  )(implicit system: ActorSystem, fmt: Format[Seq[T]]): Future[Seq[T]] = {
    implicit val materializer: ActorMaterializer = ActorMaterializer(
      ActorMaterializerSettings(system)
    )
    implicit val ec: ExecutionContext = system.dispatcher

    RestartSource
      .onFailuresWithBackoff(2.seconds, maxBackoff, 0.2, retries)(() => {
        Source.fromFuture(
          makeApiMethodRequestWithRetryAfter(apiMethod, queryParams: _*)
            .flatMap {
              case Right(jsValue) =>
                Future.successful(jsValue)
              case Left(retryAfter) =>
                akka.pattern
                  .after(retryAfter.finiteDuration, system.scheduler) {
                    Future.failed(retryAfter.invalidResponseError)
                  }
            }
        )
      })
      .runWith(Sink.head)
      .flatMap { res =>
        val nextResults = (res \ field).as[Seq[T]] ++ initialResults
        (res \ "response_metadata").asOpt[ResponseMetadata].flatMap {
          metadata =>
            metadata.next_cursor.filter(_.nonEmpty)
        } match {
          case Some(nextCursor) =>
            val newParams = queryParams.toMap + ("cursor" -> nextCursor)
            paginateCollection(
              apiMethod = apiMethod,
              queryParams = newParams.toSeq,
              field = field,
              initialResults = nextResults
            )
          case None =>
            Future.successful(nextResults)
        }
      }
  }

  private def createEntity(name: String, bytes: Array[Byte]): MessageEntity = {
    Multipart
      .FormData(
        Source.single(
          Multipart.FormData
            .BodyPart("file", HttpEntity(bytes), Map("filename" -> name))
        )
      )
      .toEntity
  }

  private def createEntity(file: File): MessageEntity = {
    Multipart
      .FormData(
        Source.single(
          Multipart.FormData.BodyPart(
            "file",
            HttpEntity.fromPath(
              MediaTypes.`application/octet-stream`,
              file.toPath,
              100000
            ),
            Map("filename" -> file.getName)
          )
        )
      )
      .toEntity
  }

  private def makeApiJsonRequest(apiMethod: String, json: JsValue)(implicit
      system: ActorSystem
  ): Future[JsValue] = {
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
}

case class InvalidResponseError(status: Int, body: String)
    extends Exception(s"Bad status code from Slack: $status")
case class ApiError(code: String) extends Exception(code)

case class HistoryChunk(
    latest: Option[String],
    messages: Seq[JsValue],
    has_more: Boolean
)

case class RepliesChunk(has_more: Boolean, messages: Seq[JsValue], ok: Boolean)

case class FileInfo(
    file: SlackFile,
    comments: Seq[SlackComment],
    paging: PagingObject
)

case class FilesResponse(files: Seq[SlackFile], paging: PagingObject)

case class ReactionsResponse(
    items: Seq[JsValue], // TODO: Parse out each object type w/ reactions
    paging: PagingObject
)

case class PagingObject(count: Int, total: Int, page: Int, pages: Int)

case class RtmStartState(
    url: String,
    self: User,
    team: Team,
    users: Seq[User],
    channels: Seq[Channel],
    groups: Seq[Group],
    ims: Seq[Im],
    bots: Seq[JsValue]
)
