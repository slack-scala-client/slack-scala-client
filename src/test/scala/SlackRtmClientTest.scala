import akka.actor.ActorSystem
import org.scalatest.FunSuite
import slack.rtm.SlackRtmClient

class SlackRtmClientTest extends FunSuite {

  implicit val system = ActorSystem("slack")
  val channel = "ABC123"
  val rtmToken = "<use rtm token to test>"

  ignore("rtm typing") {
    val rtm = SlackRtmClient(rtmToken)
    rtm.state.self.id //waiting for rtm to start
    rtm.indicateTyping(channel)
  }

  ignore("team domain") {
    val rtm = SlackRtmClient(rtmToken)
    val domain = rtm.state.team.domain
    val name = rtm.state.team.name
    assert(domain.equals("my-team"))
    assert(name.equals("My Team"))
  }


}
