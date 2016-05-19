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
}
