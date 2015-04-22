package slack

import slack.api.SlackApiClient
import scala.util.{Success,Failure}
import akka.actor._

object Main extends App {
	val token = "..."
	val system = ActorSystem("slack")

	implicit val ec = system.dispatcher

	val client = SlackApiClient(token)
	client.listChannels().onComplete {
		case Success(channels) => println(channels)
		case Failure(err) => err.printStackTrace
	}
}