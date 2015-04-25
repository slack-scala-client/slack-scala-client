package slack.rtm

import slack.api.RtmStartState
import slack.models._
import play.api.libs.json._

object RtmState {
  def apply(initial: RtmStartState): RtmState = {
    new RtmState(initial)
  }
}

class RtmState(start: RtmStartState) {
  private var _self = start.self
  private var _team = start.team
  private var _users = start.users
  private var _channels = start.channels
  private var _groups = start.groups
  private var _ims = start.ims
  private var _bots = start.bots

  def self: User = _self
  def team: Team = _team
  def users: Seq[User] = _users
  def channels: Seq[Channel] = _channels
  def groups: Seq[Group] = _groups
  def ims: Seq[Im] = _ims
  def bots: Seq[JsValue] = _bots

  def getUserIdForName(name: String): Option[String] = {
    _users.find(_.name == name).map(_.id)
  }

  def getChannelIdForName(name: String): Option[String] = {
    _channels.find(_.name == name).map(_.id)
  }

  def getUserById(id: String): Option[User] = {
    _users.find(_.id == id)
  }

  // TODO: Add remaining update events
  private[rtm] def update(event: SlackEvent) {
    event match {
      case e: ChannelCreated =>
        addReplaceChannel(e.channel)
      case e: ChannelDeleted =>
        removeChannel(e.channel)
      case e: ChannelRename =>
        addReplaceChannel(e.channel)
      case e: ImCreated =>
        addReplaceIm(e.channel)
      case e: ImClose =>
        removeIm(e.channel)
      case e: UserChange =>
        addReplaceUser(e.user)
      case e: TeamJoin =>
        addReplaceUser(e.user)
      case _ =>
    }
  }

  private[rtm] def reset(start: RtmStartState) {
    _self = start.self
    _team = start.team
    _users = start.users
    _channels = start.channels
    _groups = start.groups
    _ims = start.ims
    _bots = start.bots
  }

  private def addReplaceChannel(chan: Channel) {
    removeChannel(chan.id)
    _channels :+= chan
  }

  private def removeChannel(chanId: String) {
    _channels = _channels.filterNot(_.id == chanId)
  }

  private def addReplaceIm(im: Im) {
    removeIm(im.id)
    _ims :+= im
  }

  private def removeIm(imId: String) {
    _ims = _ims.filterNot(_.id == imId)
  }

  private def addReplaceUser(user: User) {
    removeUser(user.id)
    _users :+= user
  }

  private def removeUser(userId: String) {
    _users = _users.filterNot(_.id == userId)
  }
}