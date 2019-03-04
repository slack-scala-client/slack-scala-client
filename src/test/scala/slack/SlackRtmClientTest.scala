package slack

import java.util.concurrent.{CountDownLatch, TimeUnit}

import org.scalatest.{FunSuite, Matchers}
import slack.api.SlackApiClient
import slack.models.Reply
import slack.rtm.SlackRtmClient

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}

class SlackRtmClientTest extends FunSuite with Matchers with Credentials {

  val channel = system.settings.config.getString("test.channel")

  lazy val rtmClient = {
    val rtm = SlackRtmClient(rtmToken)
    assert(rtm.state.self.id != null)
    rtm
  }
  test("rtm typing") {
    rtmClient.indicateTyping(channel)
  }

  test("team domain") {
    val domain = rtmClient.state.team.domain
    val name = rtmClient.state.team.name
    domain should be(system.settings.config.getString("test.team.domain"))
    name should be(system.settings.config.getString("test.team.name"))
  }

  test("send message and parse reply") {
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
    val future = rtmApi.updateChatMessage(channel, "1465891701.000006", "edit-x", asUser = Some(true))
    val result = Await.result(future, 5.seconds)
    assert(result.ok.equals(true))
  }

}
