package slack.api

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import cats.Id
import slack.api.SlackApiClient.defaultSlackApiBaseUri

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object BlockingSlackApiClient {
  def apply(
    token: String,
    slackApiBaseUri: Uri = SlackApiClient.defaultSlackApiBaseUri,
    duration: FiniteDuration = 5.seconds
  ): SlackApiClientF[Id] = {
    import cats.tagless.implicits._ //fake intellij not used
    import cats._

    val fk : Future ~> Id = λ[Future ~> Id](Await.result(_, duration))
    val client = SlackApiClient(token, slackApiBaseUri)
    client.mapK(fk) //fake intellij error
  }

  def exchangeOauthForToken(
    clientId: String,
    clientSecret: String,
    code: String,
    redirectUri: Option[String] = None,
    duration: FiniteDuration = 5.seconds,
    slackApiBaseUri: Uri = defaultSlackApiBaseUri
  )(implicit system: ActorSystem): AccessToken = {
    Await.result(
      SlackApiClient.exchangeOauthForToken(clientId, clientSecret, code, redirectUri, slackApiBaseUri),
      duration
    )
  }
}
