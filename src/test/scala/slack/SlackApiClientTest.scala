package slack

import org.scalatest.FunSuite
import slack.api.SlackApiClient
import slack.models.{ActionField, Attachment}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SlackApiClientTest extends FunSuite with Credentials {

  val apiClient = SlackApiClient(token)
  val channel = system.settings.config.getString("test.channel")

  test("send attachment with action") {
    val actionField = Seq(ActionField("accept", "Accept", "button", Some("primary")))
    val attachment = Attachment(
      text = Some("Do you want to accept?"),
      fallback = Some("backup message: code-123456"),
      callback_id = Some("code-123456"),
      actions = Some(actionField)
    )

    apiClient.listChannels(1).map { channels =>
      channels.foreach(channel => println(s"${channel.id}|${channel.name}"))
    }
    val future = apiClient.postChatMessage(channel, "Request", attachments = Some(Seq(attachment)))
    val result = Await.result(future, 5.seconds)

    println(result)
  }

  test("send ephemeral with action") {
    val future = apiClient.postChatEphemeral(channel, "This is an ephemeral", user)
    val result = Await.result(future, 5.seconds)

    println(result)
  }

}
