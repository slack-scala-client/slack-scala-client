package slack

import akka.actor.ActorSystem

trait Credentials {

  implicit val system = ActorSystem("slack")
  val user = system.settings.config.getString("test.userId")
  val token = sys.env.getOrElse("SLACK_API", system.settings.config.getString("test.apiKey"))
  val rtmToken = token

}
