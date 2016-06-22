import akka.actor.ActorSystem
import org.scalatest.FunSuite
import slack.api.SlackApiClient
import slack.models.{ActionField, Attachment}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SlackApiClientTest extends FunSuite {
  implicit val system = ActorSystem("slack")
  val channel = "C14GSQ160"
  val token = "token"
  val apiClient = SlackApiClient(token)

  ignore("send attachment with action") {
    val actionField = Seq(ActionField("accept", "Accept", "button", Some("primary")))
    val attachment = Attachment(text = Some("Do you want to accept?"),
      fallback = Some("backup message: code-123456"),
      callback_id = Some("code-123456"), actions = actionField)
    val future = apiClient.postChatMessage(channel, "Request", attachments = Some(Seq(attachment)))
    val result = Await.result(future, 5.seconds)
    println(result)
  }

}