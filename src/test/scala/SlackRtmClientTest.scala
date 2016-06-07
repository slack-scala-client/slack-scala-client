import java.util.concurrent.{CountDownLatch, TimeUnit}

import akka.actor.ActorSystem
import org.scalatest.FunSuite
import slack.models.Reply
import slack.rtm.SlackRtmClient

class SlackRtmClientTest extends FunSuite {

  implicit val system = ActorSystem("slack")
  val channel = "D171GAM97"
  val rtmToken = "xoxb-41050361297-euTJacfygKYK07xQh5TnHcEC"

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

  ignore("send message and parse reply") {
    val id = 12346L
    val rtm = SlackRtmClient(rtmToken)
    assert(rtm.state.self.id != null)
    val latch = new CountDownLatch(1)
    rtm.onEvent {
      case r: Reply =>
        assert(r.reply_to.equals(id))
        latch.countDown()
      case e => println("EVENT >>>>> " + e)
    }
    rtm.sendMessage(channel, "Hi there", Some(id))
    latch.await(2, TimeUnit.SECONDS)
  }


}
