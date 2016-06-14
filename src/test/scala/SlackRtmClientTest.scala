import java.util.concurrent.{CountDownLatch, TimeUnit}

import akka.actor.ActorSystem
import org.scalatest.FunSuite
import slack.api.SlackApiClient
import slack.models.Reply
import slack.rtm.SlackRtmClient

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SlackRtmClientTest extends FunSuite {

  implicit val system = ActorSystem("slack")
  val channel = "D171GAM97"
  val rtmToken = "token"

  lazy val rtmClient = {
    val rtm = SlackRtmClient(rtmToken)
    assert(rtm.state.self.id != null)
    rtm
  }
  ignore("rtm typing") {
    rtmClient.indicateTyping(channel)
  }

  ignore("team domain") {
    val domain = rtmClient.state.team.domain
    val name = rtmClient.state.team.name
    assert(domain.equals("my-team"))
    assert(name.equals("My Team"))
  }

  ignore("send message and parse reply") {
    val id = 12346L
    val latch = new CountDownLatch(1)
    rtmClient.onEvent {
      case r: Reply =>
        assert(r.reply_to.equals(id))
        latch.countDown()
      case e => println("EVENT >>>>> " + e)
    }
    rtmClient.sendMessage(channel, "Hi there", Some(id))
    latch.await(2, TimeUnit.SECONDS)
  }

  ignore("edit message as bot") {
    val rtmApi = SlackApiClient(rtmToken)
    val future = rtmApi.updateChatMessage(channel, "1465891701.000006", "edit-x", Some(true))
    val result = Await.result(future, 5.seconds)
    assert(result.equals(true))
  }


}
