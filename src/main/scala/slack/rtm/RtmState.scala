package slack.rtm

import slack.api.RtmConnectState
import slack.models._

object RtmState {
  def apply(initial: RtmConnectState): RtmState = {
    new RtmState(initial)
  }
}

class RtmState(connect: RtmConnectState) {
  private var _self = connect.self
  private var _team = connect.team

  def self: User = _self
  def team: Team = _team

  private[rtm] def reset(start: RtmConnectState): Unit = {
    _self = start.self
    _team = start.team
  }
}