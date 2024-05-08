package slack

import org.scalatest.wordspec.AnyWordSpec

import java.io.{File, FileInputStream}
import play.api.libs.json.Json
import slack.models.SlackEvent
import slack.models.slackEventReads

class JsonMessagesDeserializationTest extends AnyWordSpec {

  "JSON message deserialization" can {
    new File("./src/test/resources/json-messages").listFiles().toList.foreach { file =>
      file.getName should {
        "be parsed without an error" in {
          Json.parse(new FileInputStream(file)).as[SlackEvent]
        }
      }
    }
  }
}
