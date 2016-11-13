import akka.actor.ActorSystem
import org.scalatest.FunSuite
import slack.api.SlackApiClient
import slack.models.{ActionField, Attachment}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SlackApiClientTest extends FunSuite {
  implicit val system = ActorSystem("slack")
  val channel = system.settings.config.getString("test.channel")
  val token =  system.settings.config.getString("test.apiKey")
  val apiClient = SlackApiClient(token)

  test("send attachment with action") {
    val actionField = Seq(ActionField("accept", "Accept", "button", Some("primary")))
    val attachment = Attachment(text = Some("Do you want to accept?"),
      fallback = Some("backup message: code-123456"),
      callback_id = Some("code-123456"), actions = actionField)

    apiClient.listChannels(1).map{ channels =>
      channels.foreach( channel => println( s"${channel.id}|${channel.name}"))
    }
    val future = apiClient.postChatMessage(channel, "Request", attachments = Some(Seq(attachment)))
    val result = Await.result(future, 5.seconds)

    println(result)
  }

}
