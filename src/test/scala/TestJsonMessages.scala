import org.scalatest.FunSuite
import play.api.libs.json.Json
import slack.models.SlackEvent

/**
 * Created by ptx on 9/5/15.
 */
class TestJsonMessages extends FunSuite {

  test("user prencese change") {

    val json = Json.parse( """{"type":"presence_change","user":"U0A2DCEBS","presence":"active"}""")
    val ev = json.as[SlackEvent]


  }

  test("channel created") {

    val json = Json.parse(
      """{"type":"channel_created","channel":{"id":"C0A76PZC0","is_channel":true,"name":"foos","created":1441461339,"creator":"U0A2DMR7F"},"event_ts":"1441461339.676215"}"""
    )
    val ev = json.as[SlackEvent]


  }



  test("channel join") {
    val json = Json.parse(
      """{
  "user": "U0A2DCEBS",
  "inviter": "U0A2DMR7F",
  "type": "message",
  "subtype": "channel_join",
  "text": "<@U0A2DCEBS|lol_bot> has joined the channel",
  "channel": "C0A77NJ22",
  "ts": "1441463918.000003"
}""")

    val ev = json.as[SlackEvent]
  }

  // :


  test("group join") {
    val json = Json.parse(
      """{
  "type": "group_joined",
  "channel": {
    "id": "G0AAYN0E7",
    "name": "secret",
    "is_group": true,
    "created": 1441743325,
    "creator": "U0A2DMR7F",
    "is_archived": false,
    "is_open": true,
    "last_read": "1441743324.000002",
    "latest": {
      "user": "U0A2DMR7F",
      "type": "message",
      "subtype": "group_join",
      "text": "<@U0A2DMR7F|ptx> has joined the group",
      "ts": "1441743324.000002"
    },
    "unread_count": 0,
    "unread_count_display": 0,
    "members": [
      "U0A2DCEBS",
      "U0A2DMR7F"
    ],
    "topic": {
      "value": "",
      "creator": "",
      "last_set": 0
    },
    "purpose": {
      "value": "",
      "creator": "",
      "last_set": 0
    }
  }
} """)

    val ev = json.as[SlackEvent]

  }

  test("group left") {
    val json = Json.parse(
      """{
      "type": "group_left", "channel": "G0AAYN0E7"
    }""")
    val ev = json.as[SlackEvent]
  }

}
