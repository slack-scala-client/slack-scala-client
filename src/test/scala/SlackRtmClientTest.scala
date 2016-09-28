import java.util.concurrent.{CountDownLatch, TimeUnit}

import slack.api.SlackApiClient
import slack.models.Reply
import slack.rtm.SlackRtmClient

import scala.concurrent.{ Await, Promise }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import org.scalatest.FunSuite
import akka.actor.ActorSystem

class SlackRtmClientTest extends FunSuite {

  implicit val system = ActorSystem("slack")
  val channel = "..."
  val rtmToken = "..."

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
    val latch = new CountDownLatch(1)
    val promise = Promise[Long]()
    rtmClient.onEvent {
      case r: Reply =>
        assert(r.reply_to.equals(Await.result(promise.future, 2.seconds)))
        latch.countDown()
      case e => println("EVENT >>>>> " + e)
    }
    val messageIdFuture = rtmClient.sendMessage(channel, "Hi there")
    promise.completeWith(messageIdFuture)
    latch.await(5, TimeUnit.SECONDS)
  }

  ignore("edit message as bot") {
    val rtmApi = SlackApiClient(rtmToken)
    val future = rtmApi.updateChatMessage(channel, "1465891701.000006", "edit-x", Some(true))
    val result = Await.result(future, 5.seconds)
    assert(result.ok.equals(true))
  }


}
