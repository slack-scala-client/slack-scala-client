package slack.api

import java.io.File
import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}
import akka.http.scaladsl.ClientTransport
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import play.api.libs.json._
import slack.models._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try


object SlackApiClient {

  private[this] val config   = ConfigFactory.load()
  private[this] val useProxy: Boolean = Try(config.getString("slack-scala-client.http.useproxy"))
    .map(_.toBoolean)
    .recover{case _:Exception => false}.getOrElse(false)

  private[this] val maybeSettings: Option[ConnectionPoolSettings] = if (useProxy) {
    val proxyHost = config.getString("slack-scala-client.http.proxyHost")
    val proxyPort = config.getString("slack-scala-client.http.proxyPort").toInt

    val httpsProxyTransport = ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(proxyHost, proxyPort))

    Some(ConnectionPoolSettings(config)
      .withConnectionSettings(ClientConnectionSettings(config)
        .withTransport(httpsProxyTransport)))
  } else {
    None
  }

  private[this] val toStrictTimeout = config.getInt("api.tostrict.timeout").seconds

  private[api] val retries: Int = config.getInt("api.retries")
  private[api] val maxBackoff: FiniteDuration = config.getInt("api.maxBackoff").seconds

  private[api] implicit val rtmStartStateFmt     = Json.format[RtmStartState]
  private[api] implicit val accessTokenFmt       = Json.format[AccessToken]
  private[api] implicit val historyChunkFmt      = Json.format[HistoryChunk]
  private[api] implicit val repliesChunkFmt      = Json.format[RepliesChunk]
  private[api] implicit val pagingObjectFmt      = Json.format[PagingObject]
  private[api] implicit val filesResponseFmt     = Json.format[FilesResponse]
  private[api] implicit val fileInfoFmt          = Json.format[FileInfo]
  private[api] implicit val reactionsResponseFmt = Json.format[ReactionsResponse]

  val defaultSlackApiBaseUri = Uri("https://slack.com/api/")

  private def createRequester(token: String, slackApiBaseUri: Uri) = new ApiRequester(
    token, slackApiBaseUri, maybeSettings, toStrictTimeout, retries, maxBackoff
  )

  def apply(token: String, slackApiBaseUri: Uri = defaultSlackApiBaseUri): SlackApiClient = {
    new SlackApiClient(createRequester(token, slackApiBaseUri))
  }

  def exchangeOauthForToken(
                             clientId: String,
                             clientSecret: String,
                             code: String,
                             redirectUri: Option[String] = None,
                             slackApiBaseUri: Uri = defaultSlackApiBaseUri
                           )(implicit system: ActorSystem): Future[AccessToken] = {
    val requester = createRequester("", slackApiBaseUri)
    val params =
      Seq("client_id" -> clientId, "client_secret" -> clientSecret, "code" -> code, "redirect_uri" -> redirectUri)
    requester.makeApiRequest(
      requester.addQueryParams(requester.addSegment(HttpRequest(uri = slackApiBaseUri), "oauth.access"), requester.cleanParams(params))
    ).map {
      case Right(jsValue) =>
        jsValue.as[AccessToken]
      case Left(retryAfter) =>
        throw retryAfter.invalidResponseError
    }(system.dispatcher)
  }

  case class SlackFileMetaData(id: Option[String],
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
                               hasRichPreview: Option[Boolean])
  case class DetailedFileInfo(ok: Option[Boolean],
                              file: SlackFileMetaData,
                              content: Option[String],
                              isTruncated: Option[Boolean],
                              comments: Option[Seq[String]])

  implicit         val SlackFileMetaDataReader: Reads[SlackFileMetaData] = new Reads[SlackFileMetaData] {
    override def reads(json: JsValue): JsResult[SlackFileMetaData] = {
      for {
        id               <- (json \ "id"               ).validateOpt[String]
        created          <- (json \ "created"          ).validateOpt[Long]
        timestamp        <- (json \ "timestamp"        ).validateOpt[Long]
        name             <- (json \ "name"             ).validateOpt[String]
        mimeType         <- (json \ "mimetype"         ).validateOpt[String]
        fileType         <- (json \ "filetype"         ).validateOpt[String]
        user             <- (json \ "user"             ).validateOpt[String]
        editable         <- (json \ "editable"         ).validateOpt[Boolean]
        size             <- (json \ "size"             ).validateOpt[Int]
        mode             <- (json \ "mode"             ).validateOpt[String]
        isExternal       <- (json \ "is_external"      ).validateOpt[Boolean]
        externalType     <- (json \ "external_type"    ).validateOpt[String]
        isPublic         <- (json \ "is_public"        ).validateOpt[Boolean]
        publicUrlShared  <- (json \ "public_url_shared").validateOpt[Boolean]
        displayAsBot     <- (json \ "display_as_bot"   ).validateOpt[Boolean]
        username         <- (json \ "username"         ).validateOpt[String]
        urlPrivate       <- (json \ "url_private"      ).validateOpt[String]
        permalink        <- (json \ "permalink"        ).validateOpt[String]
        permalinkPublic  <- (json \ "permalink_public" ).validateOpt[String]
        preview          <- (json \ "preview"          ).validateOpt[String]
        previewHighlight <- (json \ "preview_highlight").validateOpt[String]
        lines            <- (json \ "lines"            ).validateOpt[Int]
        commentsCount    <- (json \ "comments_count"   ).validateOpt[Int]
        isStarred        <- (json \ "is_starred"       ).validateOpt[Boolean]
        channels         <- (json \ "channels"         ).validateOpt[Seq[String]]
        ims              <- (json \ "ims"              ).validateOpt[Seq[String]]
        hasRichPreview   <- (json \ "has_rich_preview" ).validateOpt[Boolean]
      } yield SlackFileMetaData(id, created, timestamp, name, mimeType, fileType, user, editable, size, mode, isExternal, externalType, isPublic, publicUrlShared, displayAsBot, username, urlPrivate, permalink, permalinkPublic, preview, previewHighlight, lines, commentsCount, isStarred, channels, ims, hasRichPreview)
    }
  }
  private implicit val DetailedFileInfoReader: Reads[DetailedFileInfo] = new Reads[DetailedFileInfo] {
    override def reads(json: JsValue): JsResult[DetailedFileInfo] = {
      for {
        ok           <- (json \ "ok"          ).validateOpt[Boolean]
        fileMetaData <- (json \ "file"        ).validate[SlackFileMetaData]
        content      <- (json \ "content"     ).validateOpt[String]
        isTruncated  <- (json \ "is_truncated").validateOpt[Boolean]
        comments     <- (json \ "comments"    ).validateOpt[Seq[String]]
      } yield DetailedFileInfo(ok, fileMetaData, content, isTruncated, comments)
    }
  }
}

import slack.api.SlackApiClient._

class SlackApiClient private (requester: ApiRequester) extends SlackApiClientF[Future] {

  /**************************/
  /***   Test Endpoints   ***/
  /**************************/
  def test()(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("api.test")
    extract[Boolean](res, "ok")
  }

  def testAuth()(implicit system: ActorSystem): Future[AuthIdentity] = {
    val res = requester.makeApiMethodRequest("auth.test")
    res.map(_.as[AuthIdentity])(system.dispatcher)
  }

  /***************************/
  /***  Channel Endpoints  ***/
  /***************************/
  def archiveChannel(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("channels.archive", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def createChannel(name: String)(implicit system: ActorSystem): Future[Channel] = {
    val res = requester.makeApiMethodRequest("channels.create", "name" -> name)
    extract[Channel](res, "channel")
  }

  def getChannelHistory(channelId: String,
                        latest: Option[String] = None,
                        oldest: Option[String] = None,
                        inclusive: Option[Int] = None,
                        count: Option[Int] = None)(implicit system: ActorSystem): Future[HistoryChunk] = {
    val res = requester.makeApiMethodRequest(
      "channels.history",
      "channel" -> channelId,
      "latest" -> latest,
      "oldest" -> oldest,
      "inclusive" -> inclusive,
      "count" -> count
    )
    res.map(_.as[HistoryChunk])(system.dispatcher)
  }

  def getChannelInfo(channelId: String)(implicit system: ActorSystem): Future[Channel] = {
    val res = requester.makeApiMethodRequest("channels.info", "channel" -> channelId)
    extract[Channel](res, "channel")
  }

  def inviteToChannel(channelId: String, userId: String)(implicit system: ActorSystem): Future[Channel] = {
    val res = requester.makeApiMethodRequest("channels.invite", "channel" -> channelId, "user" -> userId)
    extract[Channel](res, "channel")
  }

  def joinChannel(channelId: String)(implicit system: ActorSystem): Future[Channel] = {
    val res = requester.makeApiMethodRequest("channels.join", "channel" -> channelId)
    extract[Channel](res, "channel")
  }

  def kickFromChannel(channelId: String, userId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("channels.kick", "channel" -> channelId, "user" -> userId)
    extract[Boolean](res, "ok")
  }

  def listChannels(excludeArchived: Boolean = false)(implicit system: ActorSystem): Future[Seq[Channel]] = {
    val res = requester.makeApiMethodRequest("channels.list", "exclude_archived" -> excludeArchived.toString)
    extract[Seq[Channel]](res, "channels")
  }

  def listConversations(channelTypes: Seq[ConversationType] = Seq(PublicChannel), excludeArchived: Int = 0)(implicit system: ActorSystem): Future[Seq[Channel]] = {
    val params = Seq(
      "exclude_archived" -> excludeArchived.toString,
      "types" -> channelTypes.map(_.conversationType).mkString(",")
    )
    requester.paginateCollection[Channel](apiMethod = "conversations.list", queryParams = params,field = "channels")
  }

  def getConversationInfo(channelId: String, includeLocale: Boolean = true, includeNumMembers: Boolean = false)(implicit system: ActorSystem): Future[Channel] = {
    val res = requester.makeApiMethodRequest("conversations.info", "channel" -> channelId, "include_locale" -> includeLocale.toString, "include_num_members" -> includeNumMembers.toString)
    extract[Channel](res, "channel")
  }

  def leaveChannel(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("channels.leave", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def markChannel(channelId: String, ts: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("channels.mark", "channel" -> channelId, "ts" -> ts)
    extract[Boolean](res, "ok")
  }

  // TODO: Lite Channel Object
  def renameChannel(channelId: String, name: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("channels.rename", "channel" -> channelId, "name" -> name)
    extract[Boolean](res, "ok")
  }

  def getChannelReplies(channelId: String, thread_ts: String)(implicit system: ActorSystem): Future[RepliesChunk] = {
    val res = requester.makeApiMethodRequest("channels.replies", "channel" -> channelId, "thread_ts" -> thread_ts)
    res.map(_.as[RepliesChunk])(system.dispatcher)
  }

  def setChannelPurpose(channelId: String, purpose: String)(implicit system: ActorSystem): Future[String] = {
    val res = requester.makeApiMethodRequest("channels.setPurpose", "channel" -> channelId, "purpose" -> purpose)
    extract[String](res, "purpose")
  }

  def setChannelTopic(channelId: String, topic: String)(implicit system: ActorSystem): Future[String] = {
    val res = requester.makeApiMethodRequest("channels.setTopic", "channel" -> channelId, "topic" -> topic)
    extract[String](res, "topic")
  }

  def unarchiveChannel(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("channels.unarchive", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  /**************************/
  /****  Chat Endpoints  ****/
  /**************************/
  def deleteChat(channelId: String, ts: String, asUser: Option[Boolean] = None)(
    implicit system: ActorSystem
  ): Future[Boolean] = {
    val params = Seq("channel" -> channelId, "ts" -> ts)
    val res = requester.makeApiMethodRequest("chat.delete", asUser.map(b => params :+ ("as_user" -> b)).getOrElse(params): _*)
    extract[Boolean](res, "ok")
  }

  def postChatEphemeral(channelId: String,
                        text: String,
                        user: String,
                        asUser: Option[Boolean] = None,
                        parse: Option[String] = None,
                        attachments: Option[Seq[Attachment]] = None,
                        blocks: Option[Seq[Block]] = None,
                        linkNames: Option[Boolean] = None)(implicit system: ActorSystem): Future[String] = {
    val json = Json.obj(
      "channel" -> channelId,
      "text" -> text,
      "user" -> user) ++
      JsObject(Seq(
        asUser.map("as_user" -> Json.toJson(_)),
        parse.map("parse" -> Json.toJson(_)),
        linkNames.map("link_names" -> Json.toJson(_)),
        attachments.map("attachments" -> Json.toJson(_)),
        blocks.map("blocks" -> Json.toJson(_))
      ).flatten)
    val res = requester.makeApiJsonRequest("chat.postEphemeral", json)
    extract[String](res, "message_ts")
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
                      replyBroadcast: Option[Boolean] = None)(implicit system: ActorSystem): Future[String] = {
    val json = Json.obj(
      "channel" -> channelId,
      "text" -> text) ++
      JsObject(Seq(
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
      ).flatten)
    val res = requester.makeApiJsonRequest("chat.postMessage", json)
    extract[String](res, "ts")
  }

  def updateChatMessage(channelId: String, ts: String, text: String,
                        attachments: Option[Seq[Attachment]] = None,
                        blocks: Option[Seq[Block]] = None,
                        parse: Option[String] = None,
                        linkNames: Option[String] = None,
                        asUser: Option[Boolean] = None,
                        threadTs: Option[String] = None)(implicit system: ActorSystem): Future[UpdateResponse] = {
    val json = Json.obj(
      "channel" -> channelId, "ts" -> ts, "text" -> text) ++
      JsObject(Seq(
        attachments.map("attachments" -> Json.toJson(_)),
        blocks.map("blocks" -> Json.toJson(_)),
        parse.map("parse" -> Json.toJson(_)),
        linkNames.map("link_names" -> Json.toJson(_)),
        asUser.map("as_user" -> Json.toJson(_)),
        threadTs.map("thread_ts" -> Json.toJson(_))
      ).flatten)
    val res = requester.makeApiJsonRequest("chat.update", json)
    res.map(_.as[UpdateResponse])(system.dispatcher)
  }

  /****************************/
  /****  Dialog Endpoints  ****/
  /****************************/
  def openDialog(triggerId: String, dialog: Dialog)(implicit system: ActorSystem): Future[Boolean] = {
    val res =
      requester.makeApiJsonRequest("dialog.open", Json.obj("trigger_id" -> triggerId, "dialog" -> Json.toJson(dialog).toString()))
    extract[Boolean](res, "ok")
  }

  /***************************/
  /****  Emoji Endpoints  ****/
  /***************************/
  def listEmojis()(implicit system: ActorSystem): Future[Map[String, String]] = {
    val res = requester.makeApiMethodRequest("emoji.list")
    extract[Map[String, String]](res, "emoji")
  }

  /**************************/
  /****  File Endpoints  ****/
  /**************************/
  def deleteFile(fileId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("files.delete", "file" -> fileId)
    extract[Boolean](res, "ok")
  }

  def getFileInfo(fileId: String, count: Option[Int] = None, page: Option[Int] = None)(
    implicit system: ActorSystem
  ): Future[FileInfo] = {
    val res = requester.makeApiMethodRequest("files.info", "file" -> fileId, "count" -> count, "page" -> page)
    res.map(_.as[FileInfo])(system.dispatcher)
  }

  def getDetailedFileInfo(file_id: String,
                          count: Option[Int] = None,
                          page: Option[Int] = None)(
                           implicit system: ActorSystem): Future[DetailedFileInfo] = {
    requester.makeApiMethodRequest("files.info", "file" -> file_id, "count" -> count, "page" -> page)
      .map(_.validate[DetailedFileInfo].get)(system.dispatcher)
  }

  def listFiles(userId: Option[String] = None,
                tsFrom: Option[String] = None,
                tsTo: Option[String] = None,
                types: Option[Seq[String]] = None,
                count: Option[Int] = None,
                page: Option[Int] = None)(implicit system: ActorSystem): Future[FilesResponse] = {
    val res = requester.makeApiMethodRequest(
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

  def uploadFile(content: Either[File, Array[Byte]],
                 filetype: Option[String] = None,
                 filename: Option[String] = None,
                 title: Option[String] = None,
                 initialComment: Option[String] = None,
                 channels: Option[Seq[String]] = None,
                 thread_ts: Option[String] = None)(implicit system: ActorSystem): Future[SlackFile] = {
    val entity = content match {
      case Right(bytes) => createEntity(filename.getOrElse("file"), bytes)
      case Left(file) => createEntity(file)
    }
    requester.uploadFileFromEntity(entity, filetype, filename, title, initialComment, channels, thread_ts)
  }

  /***************************/
  /****  Group Endpoints  ****/
  /***************************/
  def archiveGroup(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("groups.archive", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def closeGroup(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("groups.close", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def createGroup(name: String)(implicit system: ActorSystem): Future[Group] = {
    val res = requester.makeApiMethodRequest("groups.create", "name" -> name)
    extract[Group](res, "group")
  }

  def createChildGroup(channelId: String)(implicit system: ActorSystem): Future[Group] = {
    val res = requester.makeApiMethodRequest("groups.createChild", "channel" -> channelId)
    extract[Group](res, "group")
  }

  def getGroupHistory(channelId: String,
                      latest: Option[String] = None,
                      oldest: Option[String] = None,
                      inclusive: Option[Int] = None,
                      count: Option[Int] = None)(implicit system: ActorSystem): Future[HistoryChunk] = {
    val res = requester.makeApiMethodRequest(
      "groups.history",
      "channel" -> channelId,
      "latest" -> latest,
      "oldest" -> oldest,
      "inclusive" -> inclusive,
      "count" -> count
    )
    res.map(_.as[HistoryChunk])(system.dispatcher)
  }

  def getGroupInfo(channelId: String)(implicit system: ActorSystem): Future[Group] = {
    val res = requester.makeApiMethodRequest("groups.info", "channel" -> channelId)
    extract[Group](res, "group")
  }

  def inviteToGroup(channelId: String, userId: String)(implicit system: ActorSystem): Future[Group] = {
    val res = requester.makeApiMethodRequest("groups.invite", "channel" -> channelId, "user" -> userId)
    extract[Group](res, "group")
  }

  def kickFromGroup(channelId: String, userId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("groups.kick", "channel" -> channelId, "user" -> userId)
    extract[Boolean](res, "ok")
  }

  def leaveGroup(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("groups.leave", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def listGroups(excludeArchived: Int = 0)(implicit system: ActorSystem): Future[Seq[Group]] = {
    val res = requester.makeApiMethodRequest("groups.list", "exclude_archived" -> excludeArchived.toString)
    extract[Seq[Group]](res, "groups")
  }

  def markGroup(channelId: String, ts: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("groups.mark", "channel" -> channelId, "ts" -> ts)
    extract[Boolean](res, "ok")
  }

  def openGroup(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("groups.open", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  // TODO: Lite Group Object
  def renameGroup(channelId: String, name: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("groups.rename", "channel" -> channelId, "name" -> name)
    extract[Boolean](res, "ok")
  }

  def setGroupPurpose(channelId: String, purpose: String)(implicit system: ActorSystem): Future[String] = {
    val res = requester.makeApiMethodRequest("groups.setPurpose", "channel" -> channelId, "purpose" -> purpose)
    extract[String](res, "purpose")
  }

  def setGroupTopic(channelId: String, topic: String)(implicit system: ActorSystem): Future[String] = {
    val res = requester.makeApiMethodRequest("groups.setTopic", "channel" -> channelId, "topic" -> topic)
    extract[String](res, "topic")
  }

  def unarchiveGroup(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("groups.unarchive", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  /************************/
  /****  IM Endpoints  ****/
  /************************/
  def closeIm(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("im.close", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def getImHistory(channelId: String,
                   latest: Option[String] = None,
                   oldest: Option[String] = None,
                   inclusive: Option[Int] = None,
                   count: Option[Int] = None)(implicit system: ActorSystem): Future[HistoryChunk] = {
    val res = requester.makeApiMethodRequest(
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
    val res = requester.makeApiMethodRequest("im.list")
    extract[Seq[Im]](res, "ims")
  }

  def markIm(channelId: String, ts: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("im.mark", "channel" -> channelId, "ts" -> ts)
    extract[Boolean](res, "ok")
  }

  def openIm(userId: String)(implicit system: ActorSystem): Future[String] = {
    val res = requester.makeApiMethodRequest("im.open", "user" -> userId)
    res.map(r => (r \ "channel" \ "id").as[String])(system.dispatcher)
  }

  /**************************/
  /****  MPIM Endpoints  ****/
  /**************************/
  def openMpim(userIds: Seq[String])(implicit system: ActorSystem): Future[String] = {
    val res = requester.makeApiMethodRequest("mpim.open", "users" -> userIds.mkString(","))
    res.map(r => (r \ "group" \ "id").as[String])(system.dispatcher)
  }

  def closeMpim(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("mpim.close", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def listMpims()(implicit system: ActorSystem): Future[Seq[Group]] = {
    val res = requester.makeApiMethodRequest("mpim.list")
    extract[Seq[Group]](res, "groups")
  }

  def markMpim(channelId: String, ts: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("mpim.mark", "channel" -> channelId, "ts" -> ts)
    extract[Boolean](res, "ok")
  }

  def getMpimHistory(channelId: String,
                     latest: Option[String] = None,
                     oldest: Option[String] = None,
                     inclusive: Option[Int] = None,
                     count: Option[Int] = None)(implicit system: ActorSystem): Future[HistoryChunk] = {
    val res = requester.makeApiMethodRequest(
      "mpim.history",
      "channel" -> channelId,
      "latest" -> latest,
      "oldest" -> oldest,
      "inclusive" -> inclusive,
      "count" -> count
    )
    res.map(_.as[HistoryChunk])(system.dispatcher)
  }

  /******************************/
  /****  Reaction Endpoints  ****/
  /******************************/
  def addReaction(emojiName: String,
                  file: Option[String] = None,
                  fileComment: Option[String] = None,
                  channelId: Option[String] = None,
                  timestamp: Option[String] = None)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest(
      "reactions.add",
      "name" -> emojiName,
      "file" -> file,
      "file_comment" -> fileComment,
      "channel" -> channelId,
      "timestamp" -> timestamp
    )
    extract[Boolean](res, "ok")
  }

  def addReactionToMessage(emojiName: String, channelId: String, timestamp: String)(
    implicit system: ActorSystem
  ): Future[Boolean] = {
    addReaction(emojiName = emojiName, channelId = Some(channelId), timestamp = Some(timestamp))
  }

  def getReactions(file: Option[String] = None,
                   fileComment: Option[String] = None,
                   channelId: Option[String] = None,
                   timestamp: Option[String] = None,
                   full: Option[Boolean] = None)(implicit system: ActorSystem): Future[Seq[Reaction]] = {
    val res = requester.makeApiMethodRequest(
      "reactions.get",
      "file" -> file,
      "file_comment" -> fileComment,
      "channel" -> channelId,
      "timestamp" -> timestamp,
      "full" -> full
    )
    res.map(r => (r \\ "reactions").headOption.map(_.as[Seq[Reaction]]).getOrElse(Seq.empty[Reaction]))(system.dispatcher)
  }

  def getReactionsForMessage(channelId: String, timestamp: String, full: Option[Boolean] = None)(
    implicit system: ActorSystem
  ): Future[Seq[Reaction]] = {
    getReactions(channelId = Some(channelId), timestamp = Some(timestamp), full = full)
  }

  def listReactionsForUser(userId: Option[String],
                           full: Boolean = false,
                           count: Option[Int] = None,
                           page: Option[Int] = None)(implicit system: ActorSystem): Future[ReactionsResponse] = {
    val res = requester.makeApiMethodRequest("reations.list", "user" -> userId, "full" -> full, "count" -> count, "page" -> page)
    res.map(_.as[ReactionsResponse])(system.dispatcher)
  }

  def removeReaction(emojiName: String,
                     file: Option[String] = None,
                     fileComment: Option[String] = None,
                     channelId: Option[String] = None,
                     timestamp: Option[String] = None)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest(
      "reactions.remove",
      "name" -> emojiName,
      "file" -> file,
      "file_comment" -> fileComment,
      "channel" -> channelId,
      "timestamp" -> timestamp
    )
    extract[Boolean](res, "ok")
  }

  def removeReactionFromMessage(emojiName: String, channelId: String, timestamp: String)(
    implicit system: ActorSystem
  ): Future[Boolean] = {
    removeReaction(emojiName = emojiName, channelId = Some(channelId), timestamp = Some(timestamp))
  }

  /*************************/
  /****  RTM Endpoints  ****/
  /*************************/
  def startRealTimeMessageSession()(implicit system: ActorSystem): Future[RtmStartState] = {
    val res = requester.makeApiMethodRequest("rtm.start")
    res.map(_.as[RtmStartState])(system.dispatcher)
  }

  /****************************/
  /****  Search Endpoints  ****/
  /****************************/
  // TODO: Return proper search results (not JsValue)
  def searchAll(query: String,
                sort: Option[String] = None,
                sortDir: Option[String] = None,
                highlight: Option[String] = None,
                count: Option[Int] = None,
                page: Option[Int] = None)(implicit system: ActorSystem): Future[JsValue] = {
    requester.makeApiMethodRequest(
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
  def searchFiles(query: String,
                  sort: Option[String] = None,
                  sortDir: Option[String] = None,
                  highlight: Option[String] = None,
                  count: Option[Int] = None,
                  page: Option[Int] = None)(implicit system: ActorSystem): Future[JsValue] = {
    requester.makeApiMethodRequest(
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
  def searchMessages(query: String,
                     sort: Option[String] = None,
                     sortDir: Option[String] = None,
                     highlight: Option[String] = None,
                     count: Option[Int] = None,
                     page: Option[Int] = None)(implicit system: ActorSystem): Future[JsValue] = {
    requester.makeApiMethodRequest(
      "search.messages",
      "query" -> query,
      "sort" -> sort,
      "sortDir" -> sortDir,
      "highlight" -> highlight,
      "count" -> count,
      "page" -> page
    )
  }

  /***************************/
  /****  Stars Endpoints  ****/
  /***************************/
  // TODO: Return proper star items (not JsValue)
  def listStars(userId: Option[String] = None, count: Option[Int] = None, page: Option[Int] = None)(
    implicit system: ActorSystem
  ): Future[JsValue] = {
    requester.makeApiMethodRequest("start.list", "user" -> userId, "count" -> count, "page" -> page)
  }

  /**************************/
  /****  Team Endpoints  ****/
  /**************************/
  // TODO: Parse actual result type: https://api.slack.com/methods/team.accessLogs
  def getTeamAccessLogs(count: Option[Int], page: Option[Int])(implicit system: ActorSystem): Future[JsValue] = {
    requester.makeApiMethodRequest("team.accessLogs", "count" -> count, "page" -> page)
  }

  // TODO: Parse actual value type: https://api.slack.com/methods/team.info
  def getTeamInfo()(implicit system: ActorSystem): Future[JsValue] = {
    requester.makeApiMethodRequest("team.info")
  }

  /**************************/
  /****  User Endpoints  ****/
  /**************************/
  // TODO: Full payload for authed user: https://api.slack.com/methods/users.getPresence
  def getUserPresence(userId: String)(implicit system: ActorSystem): Future[String] = {
    val res = requester.makeApiMethodRequest("users.getPresence", "user" -> userId)
    extract[String](res, "presence")
  }

  def getUserInfo(userId: String)(implicit system: ActorSystem): Future[User] = {
    val res = requester.makeApiMethodRequest("users.info", "user" -> userId)
    extract[User](res, "user")
  }

  def listUsers()(implicit system: ActorSystem): Future[Seq[User]] = {
    val params = Seq("limit" -> 100)
    requester.paginateCollection[User](apiMethod = "users.list", queryParams = params,field = "members")
  }

  def setUserActive(userId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("users.setActive", "user" -> userId)
    extract[Boolean](res, "ok")
  }

  def setUserPresence(presence: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = requester.makeApiMethodRequest("users.setPresence", "presence" -> presence)
    extract[Boolean](res, "ok")
  }

  def lookupUserByEmail(emailId: String)(implicit system: ActorSystem): Future[User] = {
    val res = requester.makeApiMethodRequest("users.lookupByEmail", "email" -> emailId)
    extract[User](res, "user")
  }

  /*****************************/
  /****  Private Functions  ****/
  /*****************************/

  private def createEntity(name: String, bytes: Array[Byte]): MessageEntity = {
    Multipart
      .FormData(Source.single(Multipart.FormData.BodyPart("file", HttpEntity(bytes), Map("filename" -> name))))
      .toEntity
  }

  private def createEntity(file: File): MessageEntity = {
    Multipart
      .FormData(
        Source.single(
          Multipart.FormData.BodyPart(
            "file",
            HttpEntity.fromPath(MediaTypes.`application/octet-stream`, file.toPath, 100000),
            Map("filename" -> file.getName)
          )
        )
      )
      .toEntity
  }
}

case class InvalidResponseError(status: Int, body: String) extends Exception(s"Bad status code from Slack: $status")
case class ApiError(code: String) extends Exception(code)

case class HistoryChunk(latest: Option[String], messages: Seq[JsValue], has_more: Boolean)

case class RepliesChunk(has_more: Boolean, messages: Seq[JsValue], ok: Boolean)

case class FileInfo(file: SlackFile, comments: Seq[SlackComment], paging: PagingObject)

case class FilesResponse(files: Seq[SlackFile], paging: PagingObject)

case class ReactionsResponse(items: Seq[JsValue], // TODO: Parse out each object type w/ reactions
                             paging: PagingObject)

case class PagingObject(count: Int, total: Int, page: Int, pages: Int)

case class AccessToken(access_token: String, scope: String)

case class RtmStartState(url: String,
                         self: User,
                         team: Team,
                         users: Seq[User],
                         channels: Seq[Channel],
                         groups: Seq[Group],
                         ims: Seq[Im],
                         bots: Seq[JsValue])
