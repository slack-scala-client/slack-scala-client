package slack

import akka.actor.ActorSystem

import scala.util.Try

trait Credentials {

  implicit val system: ActorSystem = ActorSystem("slack")
  val user: Option[String] = Try(system.settings.config.getString("test.userId")).toOption
  val token: Option[String] = Try(sys.env.getOrElse("SLACK_API", system.settings.config.getString("test.apiKey"))).toOption
  val rtmToken = token

}
