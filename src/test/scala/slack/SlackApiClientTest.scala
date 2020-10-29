package slack

import slack.api.SlackApiClient
import slack.models.{ActionField, Attachment, PublicChannel}

import scala.concurrent.Await
import scala.concurrent.duration._
import org.scalatest.funsuite.AnyFunSuite

import java.util.concurrent.CountDownLatch


class SlackApiClientTest extends AnyFunSuite with Credentials {

  (user, token) match {
    case (Some(slackUser), Some(slackToken)) =>
      val apiClient = SlackApiClient(slackToken)
      val channel = system.settings.config.getString("test.channel")

      test("send attachment with action") {
        val actionField = Seq(ActionField("accept", "Accept", "button", Some("primary")))
        val attachment = Attachment(
          text = Some("Do you want to accept?"),
          fallback = Some("backup message: code-123456"),
          callback_id = Some("code-123456"),
          actions = Some(actionField)
        )

        apiClient.listChannels().map { channels =>
          channels.foreach(channel => println(s"${channel.id}|${channel.name}"))
        }
        val future = apiClient.postChatMessage(channel, "Request", attachments = Some(Seq(attachment)))
        val result = Await.result(future, 5.seconds)

        println(result)
      }

      test("list users with pagination") {
        val latch = new CountDownLatch(1)
        apiClient.listUsers().map { users =>
          println(s"Total: ${users.size} users")
          users.foreach(user => println(s"${user.id}|${user.name}|${user.is_bot}|${user.is_admin}"))
          latch.countDown()
        }
        latch.await()
      }

      test("list channels using conversations.list") {
        val latch = new CountDownLatch(1)
        apiClient.listConversations(Seq(PublicChannel)).map { channels =>
          println(s"Total: ${channels.size} channels")
          channels.foreach(channel => println(s"${channel.id}|${channel.name}|${channel.is_private}|${channel.is_member}"))
          latch.countDown()
        }
        latch.await()
      }


    case _ =>
      println("Skipping the test as the API credentials are not available")
  }
}
