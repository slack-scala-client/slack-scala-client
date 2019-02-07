package slack.api

import slack.models._

import java.io.File
import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.http.javadsl.model.headers.ContentType
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.parboiled2.CharPredicate
import play.api.libs.json._

object SlackApiClient {

  private[api] implicit val rtmStartStateFmt = Json.format[RtmStartState]
  private[api] implicit val accessTokenFmt = Json.format[AccessToken]
  private[api] implicit val historyChunkFmt = Json.format[HistoryChunk]
  private[api] implicit val repliesChunkFmt = Json.format[RepliesChunk]
  private[api] implicit val pagingObjectFmt = Json.format[PagingObject]
  private[api] implicit val filesResponseFmt = Json.format[FilesResponse]
  private[api] implicit val fileInfoFmt = Json.format[FileInfo]
  private[api] implicit val reactionsResponseFmt = Json.format[ReactionsResponse]

  val defaultSlackApiBaseUri = Uri("https://slack.com/api/")

  /* TEMPORARY WORKAROUND - UrlEncode '?' in query string parameters */
  val charClassesClass = Class.forName("akka.http.impl.model.parser.CharacterClasses$")
  val charClassesObject = charClassesClass.getField("MODULE$").get(charClassesClass)
  //  strict-query-char-np
  val charPredicateField = charClassesObject.getClass.getDeclaredField("strict$minusquery$minuschar$minusnp")
  charPredicateField.setAccessible(true)
  val updatedCharPredicate = charPredicateField.get(charClassesObject).asInstanceOf[CharPredicate] -- '?'
  charPredicateField.set(charClassesObject, updatedCharPredicate)
  /* END TEMPORARY WORKAROUND */

  def apply(token: String, slackApiBaseUri: Uri = defaultSlackApiBaseUri): SlackApiClient = {
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
      Seq("client_id" -> clientId, "client_secret" -> clientSecret, "code" -> code, "redirect_uri" -> redirectUri)
    val res = makeApiRequest(
      addQueryParams(addSegment(HttpRequest(uri = slackApiBaseUri), "oauth.access"), cleanParams(params))
    )
    res.map(_.as[AccessToken])(system.dispatcher)
  }

  private def makeApiRequest(request: HttpRequest)(implicit system: ActorSystem): Future[JsValue] = {
    implicit val mat = ActorMaterializer()
    implicit val ec = system.dispatcher
    Http().singleRequest(request).flatMap {
      case response if response.status.intValue == 200 =>
        response.entity.toStrict(10.seconds).map { entity =>
          val parsed = Json.parse(entity.data.decodeString("UTF-8"))
          if ((parsed \ "ok").as[Boolean]) {
            parsed
          } else {
            throw ApiError((parsed \ "error").as[String])
          }
        }
      case response =>
        response.entity.toStrict(10.seconds).map { entity =>
          throw InvalidResponseError(response.status.intValue, entity.data.decodeString("UTF-8"))
        }
    }
  }

  private def extract[T](jsFuture: Future[JsValue], field: String)(implicit system: ActorSystem,
                                                                   fmt: Format[T]): Future[T] = {
    jsFuture.map(js => (js \ field).as[T])(system.dispatcher)
  }

  private def addQueryParams(request: HttpRequest, queryParams: Seq[(String, String)]): HttpRequest = {
    request.withUri(request.uri.withQuery(Uri.Query((request.uri.query() ++ queryParams): _*)))
  }

  private def cleanParams(params: Seq[(String, Any)]): Seq[(String, String)] = {
    var paramList = Seq[(String, String)]()
    params.foreach {
      case (k, Some(v)) => paramList :+= (k -> v.toString)
      case (k, None) => // Nothing - Filter out none
      case (k, v) => paramList :+= (k -> v.toString)
    }
    paramList
  }

  private def addSegment(request: HttpRequest, segment: String): HttpRequest = {
    request.withUri(request.uri.withPath(request.uri.path + segment))
  }
}

import slack.api.SlackApiClient._

class SlackApiClient private (token: String, slackApiBaseUri: Uri) {

  private val apiBaseRequest = HttpRequest(uri = slackApiBaseUri)

  private val apiBaseWithTokenRequest = apiBaseRequest.withUri(
    apiBaseRequest.uri.withQuery(Uri.Query((apiBaseRequest.uri.query() :+ ("token" -> token)): _*))
  )

  /**************************/
  /***   Test Endpoints   ***/
  /**************************/
  def test()(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("api.test")
    extract[Boolean](res, "ok")
  }

  def testAuth()(implicit system: ActorSystem): Future[AuthIdentity] = {
    val res = makeApiMethodRequest("auth.test")
    res.map(_.as[AuthIdentity])(system.dispatcher)
  }

  /***************************/
  /***  Channel Endpoints  ***/
  /***************************/
  def archiveChannel(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("channels.archive", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def createChannel(name: String)(implicit system: ActorSystem): Future[Channel] = {
    val res = makeApiMethodRequest("channels.create", "name" -> name)
    extract[Channel](res, "channel")
  }

  def getChannelHistory(channelId: String,
                        latest: Option[String] = None,
                        oldest: Option[String] = None,
                        inclusive: Option[Int] = None,
                        count: Option[Int] = None)(implicit system: ActorSystem): Future[HistoryChunk] = {
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

  def getChannelInfo(channelId: String)(implicit system: ActorSystem): Future[Channel] = {
    val res = makeApiMethodRequest("channels.info", "channel" -> channelId)
    extract[Channel](res, "channel")
  }

  def inviteToChannel(channelId: String, userId: String)(implicit system: ActorSystem): Future[Channel] = {
    val res = makeApiMethodRequest("channels.invite", "channel" -> channelId, "user" -> userId)
    extract[Channel](res, "channel")
  }

  def joinChannel(channelId: String)(implicit system: ActorSystem): Future[Channel] = {
    val res = makeApiMethodRequest("channels.join", "channel" -> channelId)
    extract[Channel](res, "channel")
  }

  def kickFromChannel(channelId: String, userId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("channels.kick", "channel" -> channelId, "user" -> userId)
    extract[Boolean](res, "ok")
  }

  def listChannels(excludeArchived: Int = 0)(implicit system: ActorSystem): Future[Seq[Channel]] = {
    val res = makeApiMethodRequest("channels.list", "exclude_archived" -> excludeArchived.toString)
    extract[Seq[Channel]](res, "channels")
  }

  def leaveChannel(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("channels.leave", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def markChannel(channelId: String, ts: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("channels.mark", "channel" -> channelId, "ts" -> ts)
    extract[Boolean](res, "ok")
  }

  // TODO: Lite Channel Object
  def renameChannel(channelId: String, name: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("channels.rename", "channel" -> channelId, "name" -> name)
    extract[Boolean](res, "ok")
  }

  def getChannelReplies(channelId: String, thread_ts: String)(implicit system: ActorSystem): Future[RepliesChunk] = {
    val res = makeApiMethodRequest("channels.replies", "channel" -> channelId, "thread_ts" -> thread_ts)
    res.map(_.as[RepliesChunk])(system.dispatcher)
  }

  def setChannelPurpose(channelId: String, purpose: String)(implicit system: ActorSystem): Future[String] = {
    val res = makeApiMethodRequest("channels.setPurpose", "channel" -> channelId, "purpose" -> purpose)
    extract[String](res, "purpose")
  }

  def setChannelTopic(channelId: String, topic: String)(implicit system: ActorSystem): Future[String] = {
    val res = makeApiMethodRequest("channels.setTopic", "channel" -> channelId, "topic" -> topic)
    extract[String](res, "topic")
  }

  def unarchiveChannel(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("channels.unarchive", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  /**************************/
  /****  Chat Endpoints  ****/
  /**************************/
  def deleteChat(channelId: String, ts: String, asUser: Option[Boolean] = None)(
    implicit system: ActorSystem
  ): Future[Boolean] = {
    val params = Seq("channel" -> channelId, "ts" -> ts)
    val res = makeApiMethodRequest("chat.delete", asUser.map(b => params :+ ("as_user" -> b)).getOrElse(params): _*)
    extract[Boolean](res, "ok")
  }

  def postChatEphemeral(channelId: String,
                        text: String,
                        user: String,
                        asUser: Option[Boolean] = None,
                        parse: Option[String] = None,
                        attachments: Option[Seq[Attachment]] = None,
                        linkNames: Option[Boolean] = None)(implicit system: ActorSystem): Future[String] = {
    val res = makeApiMethodRequest(
      "chat.postEphemeral",
      "channel" -> channelId,
      "text" -> text,
      "as_user" -> asUser,
      "user" -> user,
      "parse" -> parse,
      "link_names" -> linkNames,
      "attachments" -> attachments.map(a => Json.stringify(Json.toJson(a)))
    )
    extract[String](res, "message_ts")
  }

  def postChatMessage(channelId: String,
                      text: String,
                      username: Option[String] = None,
                      asUser: Option[Boolean] = None,
                      parse: Option[String] = None,
                      linkNames: Option[String] = None,
                      attachments: Option[Seq[Attachment]] = None,
                      unfurlLinks: Option[Boolean] = None,
                      unfurlMedia: Option[Boolean] = None,
                      iconUrl: Option[String] = None,
                      iconEmoji: Option[String] = None,
                      replaceOriginal: Option[Boolean] = None,
                      deleteOriginal: Option[Boolean] = None,
                      threadTs: Option[String] = None)(implicit system: ActorSystem): Future[String] = {
    val res = makeApiMethodRequest(
      "chat.postMessage",
      "channel" -> channelId,
      "text" -> text,
      "username" -> username,
      "as_user" -> asUser,
      "parse" -> parse,
      "link_names" -> linkNames,
      "attachments" -> attachments.map(a => Json.stringify(Json.toJson(a))),
      "unfurl_links" -> unfurlLinks,
      "unfurl_media" -> unfurlMedia,
      "icon_url" -> iconUrl,
      "icon_emoji" -> iconEmoji,
      "replace_original" -> replaceOriginal,
      "delete_original" -> deleteOriginal,
      "thread_ts" -> threadTs
    )
    extract[String](res, "ts")
  }

  def updateChatMessage(channelId: String, ts: String, text: String, asUser: Option[Boolean] = None)(
    implicit system: ActorSystem
  ): Future[UpdateResponse] = {
    val params = Seq("channel" -> channelId, "ts" -> ts, "text" -> text)
    val res = makeApiMethodRequest("chat.update", asUser.map(b => params :+ ("as_user" -> b)).getOrElse(params): _*)
    res.map(_.as[UpdateResponse])(system.dispatcher)
  }

  /****************************/
  /****  Dialog Endpoints  ****/
  /****************************/
  def openDialog(triggerId: String, dialog: Dialog)(implicit system: ActorSystem): Future[Boolean] = {
    val res =
      makeApiJsonRequest("dialog.open", Json.obj("trigger_id" -> triggerId, "dialog" -> Json.toJson(dialog).toString()))
    extract[Boolean](res, "ok")
  }

  /***************************/
  /****  Emoji Endpoints  ****/
  /***************************/
  def listEmojis()(implicit system: ActorSystem): Future[Map[String, String]] = {
    val res = makeApiMethodRequest("emoji.list")
    extract[Map[String, String]](res, "emoji")
  }

  /**************************/
  /****  File Endpoints  ****/
  /**************************/
  def deleteFile(fileId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("files.delete", "file" -> fileId)
    extract[Boolean](res, "ok")
  }

  def getFileInfo(fileId: String, count: Option[Int] = None, page: Option[Int] = None)(
    implicit system: ActorSystem
  ): Future[FileInfo] = {
    val res = makeApiMethodRequest("files.info", "file" -> fileId, "count" -> count, "page" -> page)
    res.map(_.as[FileInfo])(system.dispatcher)
  }

  def listFiles(userId: Option[String] = None,
                tsFrom: Option[String] = None,
                tsTo: Option[String] = None,
                types: Option[Seq[String]] = None,
                count: Option[Int] = None,
                page: Option[Int] = None)(implicit system: ActorSystem): Future[FilesResponse] = {
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
    uploadFileFromEntity(entity, filetype, filename, title, initialComment, channels, thread_ts)
  }

  /***************************/
  /****  Group Endpoints  ****/
  /***************************/
  def archiveGroup(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("groups.archive", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def closeGroup(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("groups.close", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def createGroup(name: String)(implicit system: ActorSystem): Future[Group] = {
    val res = makeApiMethodRequest("groups.create", "name" -> name)
    extract[Group](res, "group")
  }

  def createChildGroup(channelId: String)(implicit system: ActorSystem): Future[Group] = {
    val res = makeApiMethodRequest("groups.createChild", "channel" -> channelId)
    extract[Group](res, "group")
  }

  def getGroupHistory(channelId: String,
                      latest: Option[String] = None,
                      oldest: Option[String] = None,
                      inclusive: Option[Int] = None,
                      count: Option[Int] = None)(implicit system: ActorSystem): Future[HistoryChunk] = {
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

  def getGroupInfo(channelId: String)(implicit system: ActorSystem): Future[Group] = {
    val res = makeApiMethodRequest("groups.info", "channel" -> channelId)
    extract[Group](res, "group")
  }

  def inviteToGroup(channelId: String, userId: String)(implicit system: ActorSystem): Future[Group] = {
    val res = makeApiMethodRequest("groups.invite", "channel" -> channelId, "user" -> userId)
    extract[Group](res, "group")
  }

  def kickFromGroup(channelId: String, userId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("groups.kick", "channel" -> channelId, "user" -> userId)
    extract[Boolean](res, "ok")
  }

  def leaveGroup(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("groups.leave", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def listGroups(excludeArchived: Int = 0)(implicit system: ActorSystem): Future[Seq[Group]] = {
    val res = makeApiMethodRequest("groups.list", "exclude_archived" -> excludeArchived.toString)
    extract[Seq[Group]](res, "groups")
  }

  def markGroup(channelId: String, ts: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("groups.mark", "channel" -> channelId, "ts" -> ts)
    extract[Boolean](res, "ok")
  }

  def openGroup(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("groups.open", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  // TODO: Lite Group Object
  def renameGroup(channelId: String, name: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("groups.rename", "channel" -> channelId, "name" -> name)
    extract[Boolean](res, "ok")
  }

  def setGroupPurpose(channelId: String, purpose: String)(implicit system: ActorSystem): Future[String] = {
    val res = makeApiMethodRequest("groups.setPurpose", "channel" -> channelId, "purpose" -> purpose)
    extract[String](res, "purpose")
  }

  def setGroupTopic(channelId: String, topic: String)(implicit system: ActorSystem): Future[String] = {
    val res = makeApiMethodRequest("groups.setTopic", "channel" -> channelId, "topic" -> topic)
    extract[String](res, "topic")
  }

  def unarchiveGroup(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("groups.unarchive", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  /************************/
  /****  IM Endpoints  ****/
  /************************/
  def closeIm(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("im.close", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def getImHistory(channelId: String,
                   latest: Option[String] = None,
                   oldest: Option[String] = None,
                   inclusive: Option[Int] = None,
                   count: Option[Int] = None)(implicit system: ActorSystem): Future[HistoryChunk] = {
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

  def markIm(channelId: String, ts: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("im.mark", "channel" -> channelId, "ts" -> ts)
    extract[Boolean](res, "ok")
  }

  def openIm(userId: String)(implicit system: ActorSystem): Future[String] = {
    val res = makeApiMethodRequest("im.open", "user" -> userId)
    res.map(r => (r \ "channel" \ "id").as[String])(system.dispatcher)
  }

  /**************************/
  /****  MPIM Endpoints  ****/
  /**************************/
  def openMpim(userIds: Seq[String])(implicit system: ActorSystem): Future[String] = {
    val res = makeApiMethodRequest("mpim.open", "users" -> userIds.mkString(","))
    res.map(r => (r \ "group" \ "id").as[String])(system.dispatcher)
  }

  def closeMpim(channelId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("mpim.close", "channel" -> channelId)
    extract[Boolean](res, "ok")
  }

  def listMpims()(implicit system: ActorSystem): Future[Seq[Group]] = {
    val res = makeApiMethodRequest("mpim.list")
    extract[Seq[Group]](res, "groups")
  }

  def markMpim(channelId: String, ts: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("mpim.mark", "channel" -> channelId, "ts" -> ts)
    extract[Boolean](res, "ok")
  }

  def getMpimHistory(channelId: String,
                     latest: Option[String] = None,
                     oldest: Option[String] = None,
                     inclusive: Option[Int] = None,
                     count: Option[Int] = None)(implicit system: ActorSystem): Future[HistoryChunk] = {
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

  /******************************/
  /****  Reaction Endpoints  ****/
  /******************************/
  def addReaction(emojiName: String,
                  file: Option[String] = None,
                  fileComment: Option[String] = None,
                  channelId: Option[String] = None,
                  timestamp: Option[String] = None)(implicit system: ActorSystem): Future[Boolean] = {
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
    val res = makeApiMethodRequest(
      "reactions.get",
      "file" -> file,
      "file_comment" -> fileComment,
      "channel" -> channelId,
      "timestamp" -> timestamp,
      "full" -> full
    )
    res.map(r => (r \\ "reactions").headOption.map(_.as[Seq[Reaction]]).getOrElse(Seq[Reaction]()))(system.dispatcher)
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
    val res = makeApiMethodRequest("reations.list", "user" -> userId, "full" -> full, "count" -> count, "page" -> page)
    res.map(_.as[ReactionsResponse])(system.dispatcher)
  }

  def removeReaction(emojiName: String,
                     file: Option[String] = None,
                     fileComment: Option[String] = None,
                     channelId: Option[String] = None,
                     timestamp: Option[String] = None)(implicit system: ActorSystem): Future[Boolean] = {
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

  def removeReactionFromMessage(emojiName: String, channelId: String, timestamp: String)(
    implicit system: ActorSystem
  ): Future[Boolean] = {
    removeReaction(emojiName = emojiName, channelId = Some(channelId), timestamp = Some(timestamp))
  }

  /*************************/
  /****  RTM Endpoints  ****/
  /*************************/
  def startRealTimeMessageSession()(implicit system: ActorSystem): Future[RtmStartState] = {
    val res = makeApiMethodRequest("rtm.start")
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
  def searchFiles(query: String,
                  sort: Option[String] = None,
                  sortDir: Option[String] = None,
                  highlight: Option[String] = None,
                  count: Option[Int] = None,
                  page: Option[Int] = None)(implicit system: ActorSystem): Future[JsValue] = {
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
  def searchMessages(query: String,
                     sort: Option[String] = None,
                     sortDir: Option[String] = None,
                     highlight: Option[String] = None,
                     count: Option[Int] = None,
                     page: Option[Int] = None)(implicit system: ActorSystem): Future[JsValue] = {
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

  /***************************/
  /****  Stars Endpoints  ****/
  /***************************/
  // TODO: Return proper star items (not JsValue)
  def listStars(userId: Option[String] = None, count: Option[Int] = None, page: Option[Int] = None)(
    implicit system: ActorSystem
  ): Future[JsValue] = {
    makeApiMethodRequest("start.list", "user" -> userId, "count" -> count, "page" -> page)
  }

  /**************************/
  /****  Team Endpoints  ****/
  /**************************/
  // TODO: Parse actual result type: https://api.slack.com/methods/team.accessLogs
  def getTeamAccessLogs(count: Option[Int], page: Option[Int])(implicit system: ActorSystem): Future[JsValue] = {
    makeApiMethodRequest("team.accessLogs", "count" -> count, "page" -> page)
  }

  // TODO: Parse actual value type: https://api.slack.com/methods/team.info
  def getTeamInfo()(implicit system: ActorSystem): Future[JsValue] = {
    makeApiMethodRequest("team.info")
  }

  /**************************/
  /****  User Endpoints  ****/
  /**************************/
  // TODO: Full payload for authed user: https://api.slack.com/methods/users.getPresence
  def getUserPresence(userId: String)(implicit system: ActorSystem): Future[String] = {
    val res = makeApiMethodRequest("users.getPresence", "user" -> userId)
    extract[String](res, "presence")
  }

  def getUserInfo(userId: String)(implicit system: ActorSystem): Future[User] = {
    val res = makeApiMethodRequest("users.info", "user" -> userId)
    extract[User](res, "user")
  }

  def listUsers()(implicit system: ActorSystem): Future[Seq[User]] = {
    val res = makeApiMethodRequest("users.list")
    extract[Seq[User]](res, "members")
  }

  def setUserActive(userId: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("users.setActive", "user" -> userId)
    extract[Boolean](res, "ok")
  }

  def setUserPresence(presence: String)(implicit system: ActorSystem): Future[Boolean] = {
    val res = makeApiMethodRequest("users.setPresence", "presence" -> presence)
    extract[Boolean](res, "ok")
  }

  def lookupUserByEmail(emailId: String)(implicit system: ActorSystem): Future[User] = {
    val res = makeApiMethodRequest("users.lookupByEmail", "email" -> emailId)
    extract[User](res, "user")
  }

  /*****************************/
  /****  Private Functions  ****/
  /*****************************/
  private def uploadFileFromEntity(entity: MessageEntity,
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
    val res = makeApiRequest(addQueryParams(request, cleanParams(params)))
    extract[SlackFile](res, "file")
  }

  private def makeApiMethodRequest(apiMethod: String,
                                   queryParams: (String, Any)*)(implicit system: ActorSystem): Future[JsValue] = {
    val req = addSegment(apiBaseWithTokenRequest, apiMethod)
    makeApiRequest(addQueryParams(req, cleanParams(queryParams)))
  }

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

  private def makeApiJsonRequest(apiMethod: String, json: JsValue)(implicit system: ActorSystem): Future[JsValue] = {
    val req = addSegment(apiBaseRequest, apiMethod)
      .withMethod(HttpMethods.POST)
      .withHeaders(ContentType.create(ContentTypes.`application/json`), Authorization(OAuth2BearerToken(token)))
      .withEntity(json.toString())
    makeApiRequest(req)
  }
}

case class InvalidResponseError(status: Int, body: String) extends Exception(s"Bad status code from Slack: ${status}")
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
