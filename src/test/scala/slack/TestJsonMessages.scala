package slack

import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.Json
import slack.models.MessageSubtypes.FileShareMessage
import slack.models._

import scala.io.Source

/**
  * Created by ptx on 9/5/15.
  */
class TestJsonMessages extends FunSuite with Matchers {

  test("user presence change") {

    val json = Json.parse("""{"type":"presence_change","user":"U0A2DCEBS","presence":"active"}""")
    json.as[SlackEvent]
  }

  test("channel created") {

    val json = Json.parse(
      """{"type":"channel_created","channel":{"id":"C0A76PZC0","is_channel":true,"name":"foos","created":1441461339,"creator":"U0A2DMR7F"},"event_ts":"1441461339.676215"}"""
    )
    json.as[SlackEvent]
  }

  test("channel join") {
    val json = Json.parse("""{
  "user": "U0A2DCEBS",
  "inviter": "U0A2DMR7F",
  "type": "message",
  "subtype": "channel_join",
  "text": "<@U0A2DCEBS|lol_bot> has joined the channel",
  "channel": "C0A77NJ22",
  "ts": "1441463918.000003"
}""")

    json.as[SlackEvent]
  }

  // :

  test("group join") {
    val json = Json.parse("""{
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

    json.as[SlackEvent]
  }

  test("group left") {
    val json = Json.parse("""{
      "type": "group_left", "channel": "G0AAYN0E7"
    }""")
    json.as[SlackEvent]
  }

  test("me message parsed") {
    val json = Json.parse("""{
        |  "type":"message",
        |  "user":"U0A2DCEBS",
        |  "channel":"G0AAYN0E7",
        |  "text": "Cheers!",
        |  "subtype":"me_message"
        |}""".stripMargin)
    json.as[MessageSubtypes.MeMessage]
  }

  test("unhandled message parsed") {
    val json = Json.parse("""{
        |  "type":"message",
        |  "user":"U0A2DCEBS",
        |  "channel":"G0AAYN0E7",
        |  "text": "An ordinary box for pizza.",
        |  "subtype":"pizza_box"
        |}""".stripMargin)
    json.as[MessageSubtypes.UnhandledSubtype]
  }

  test("message_changed event parsed") {
    val json = Json.parse("""{
        |  "type":"message",
        |  "message":{
        |    "type":"message",
        |    "user":"U0W6K3Y6T",
        |    "text":"Hi",
        |    "edited":{
        |      "user":"U0W6K3Y6T",
        |      "ts":"1461159087.000000"
        |    },
        |    "ts":"1461159085.000005"
        |  },
        |  "subtype":"message_changed",
        |  "hidden":true,
        |  "channel":"G1225QJGJ",
        |  "previous_message":{
        |    "type":"message",
        |    "user":"U0W6K3Y6T",
        |    "text":"Hii",
        |    "ts":"1461159085.000005"
        |  },
        |  "event_ts":"1461159087.697321",
        |  "ts":"1461159087.000006"
        |}""".stripMargin)
    json.as[MessageChanged]
  }

  test("parse additional params in channel") {
    val json = Json.parse(
      """{"type": "group_joined", "channel": {"topic": {"last_set": 0, "value": "", "creator": ""},
        |"name": "test-2", "last_read": "1461761466.000002", "creator": "U0T2SJ99Q", "is_mpim": false, "is_archived": false,
        |"created": 1461761466, "is_group": true, "members": ["U0T2SJ99Q", "U12NQNABX"], "unread_count": 0, "is_open": true,
        |"purpose": {"last_set": 0, "value": "", "creator": ""}, "unread_count_display": 0, "id": "G145D40VC"}}""".stripMargin
    )
    val ev = json.as[GroupJoined]
    ev.channel.is_mpim should be(Some(false))
    ev.channel.is_group should be(Some(true))
    ev.channel.is_channel.isEmpty should be(true)
  }

  test("parse bot message") {
    val json = Json.parse(
      """{"text":"bot message","username":"mybot","bot_id":"B1E2Y493N","type":"message","subtype":"bot_message","team":"T0P3TAZ7Y",
        |"user_profile":{"avatar_hash":null,"image_72":"https://secure.gravatar.com/avatar/d41d8cd98f00b204e9800998ecf8427e.jpg?s=72&d=https%3A%2F%2Fa.slack-edge.com%2F66f9%2Fimg%2Favatars%2Fava_0000-72.png",
        |"first_name":null,"real_name":"","name":null},"channel":"D1632C4LU","ts":"1464985393.000154"}""".stripMargin
    )
    val ev = json.as[BotMessage]
    ev.bot_id should be("B1E2Y493N")
  }

  test("parse bot message with attachment") {
    val json = Json.parse(
      """{"text":"bot message","username":"mybot","bot_id":"B1E2Y493N","type":"message","subtype":"bot_message","team":"T0P3TAZ7Y",
        |"user_profile":{"avatar_hash":null,"image_72":"https://secure.gravatar.com/avatar/d41d8cd98f00b204e9800998ecf8427e.jpg?s=72&d=https%3A%2F%2Fa.slack-edge.com%2F66f9%2Fimg%2Favatars%2Fava_0000-72.png",
        |"first_name":null,"real_name":"","name":null},"channel":"D1632C4LU","ts":"1464985393.000154",
        |"attachments":[{"text": "Don't get too attached", "fallback": "This is an attachment fallback"}]}""".stripMargin
    )
    val ev = json.as[BotMessage]
    ev.bot_id should be("B1E2Y493N")
  }

  test("parse file share message") {
    val json = Json.parse(
      """{"username": "<@U0X6J06MD|super-roger>", "display_as_bot": false,
        |  "text": "<@U0X6J06MD|super-roger> uploaded a file: <https://okroger-agents-dev.slack.com/files/super-roger/F1FVBN542/ok.png|ok>",
        |   "upload": true, "ts": "1465589621.000008", "subtype": "file_share", "user": "U0X6J06MD",
        |   "file": { "id": "F1FVBN542", "created":1465567656, "timestamp": 1465569974, "name":"test-file", "title":"test-title",
        |   "mimetype":"image/png","filetype":"image/png","pretty_type":"test", "user":"U1234", "mode":"test-mode",
        |   "editable":false,"is_external":false, "external_type":"etype", "size":2000}, "team": "T0W6887JS",
        |         "type": "message", "channel": "G172PTNSH"}""".stripMargin
    )
    val ev = json.as[SlackEvent]
    ev.asInstanceOf[MessageWithSubtype].messageSubType should be(
      FileShareMessage(
        SlackFile(
          "F1FVBN542",
          1465567656,
          1465569974,
          Some("test-file"),
          "test-title",
          "image/png",
          "image/png",
          "test",
          "U1234",
          "test-mode",
          editable = false,
          is_external = false,
          "etype",
          2000,
          None,
          None,
          None,
          None,
          None
        )
      )
    )
  }

  test("parse reaction added to message") {
    val json = Json.parse("""{"type":"reaction_added","user":"U024BE7LH","reaction":"thumbsup","item_user":"U0G9QF9C6",
        |"item":{"type":"message","channel":"C0G9QF9GZ","ts":"1360782400.498405"},
        |"event_ts":"1360782804.083113"}""".stripMargin)
    val ev = json.as[SlackEvent]
    ev should be(
      ReactionAdded(
        "thumbsup",
        ReactionItemMessage("C0G9QF9GZ", "1360782400.498405"),
        "1360782804.083113",
        "U024BE7LH",
        Some("U0G9QF9C6")
      )
    )
  }

  test("parse reaction added to file") {
    val json = Json.parse("""{"type":"reaction_added","user":"U024BE7LH","reaction":"thumbsup","item_user":"U0G9QF9C6",
        |"item":{"type":"file","file":"F0HS27V1Z"},
        |"event_ts":"1360782804.083113"}""".stripMargin)
    val ev = json.as[SlackEvent]
    ev should be(
      ReactionAdded("thumbsup", ReactionItemFile("F0HS27V1Z"), "1360782804.083113", "U024BE7LH", Some("U0G9QF9C6"))
    )
  }

  test("parse reaction removed from file comment") {
    val json =
      Json.parse("""{"type":"reaction_removed","user":"U024BE7LH","reaction":"thumbsup","item_user":"U0G9QF9C6",
        |"item":{"type":"file_comment","file":"F0HS27V1Z","file_comment": "FC0HS2KBEZ"},
        |"event_ts":"1360782804.083113"}""".stripMargin)
    val ev = json.as[SlackEvent]
    ev should be(
      ReactionRemoved(
        "thumbsup",
        ReactionItemFileComment("F0HS27V1Z", "FC0HS2KBEZ"),
        "1360782804.083113",
        "U024BE7LH",
        Some("U0G9QF9C6")
      )
    )
  }

  test("user dnd status updated") {
    val json =
      Json.parse("""{"type":"dnd_updated_user","user":"U024BE7LH",
        |"dnd_status":{"dnd_enabled":true,"next_dnd_start_ts":1515016800,"next_dnd_end_ts":1515052800},
        |"event_ts":"1514991882.000376"}""".stripMargin)
    val ev = json.as[SlackEvent]
    ev should be(
      DndUpdatedUser(
        "dnd_updated_user",
        "U024BE7LH",
        DndStatus(dnd_enabled = true, 1515016800, 1515052800),
        "1514991882.000376"
      )
    )
  }

  test("member joined channel") {
    val json = Json.parse("""
        |{"type":"member_joined_channel","user":"U0G9QF9C6","channel":"C0A76PZC0","channel_type":"C",
        |"team":"T0P3TAZ7Y","inviter":"U024BE7LH","event_ts":"1521143660.000263",
        |"ts":"1521143660.000263"}""".stripMargin)
    val ev = json.as[MemberJoined]
    ev.inviter.get should be("U024BE7LH")
  }

  test("member left channel") {
    val json = Json.parse("""
        |{"type":"member_left_channel","user":"U0G9QF9C6","channel":"C0A76PZC0","channel_type":"C",
        |"team":"T0PTEAMEV","event_ts":"1540949060.060700","ts":"1540949060.060700"}
      """.stripMargin)

    val ev = json.as[MemberLeft]
    ev.user should be("U0G9QF9C6")
  }

  test("channel renamed") {
    val json = Json.parse("""
        |{"type":"channel_rename","channel":{"id":"CEA504436","is_channel":true,
        |"name":"newname","name_normalized":"newname","created":1542210850},
        |"event_ts":"1542211100.033200"}
      """.stripMargin)
    val ev = json.as[ChannelRename]
    ev.channel.name should be("newname")
  }

  test("message replied") {
    val json = Json.parse("""
        |{"type":"message","message":{"type":"message","user":"U1ABCDE92",
        |"text":"<@U8P8P8PAG> Let me know when you are ready",
        |"client_msg_id":"11111111-2222-4444-aaaa-020202020202","thread_ts":"1543236821.311700",
        |"reply_count":3,"replies":[{"user":"U8P8P8PAG","ts":"1543236934.312100"},
        |{"user":"U935935EC","ts":"1543240626.312400"},{"user":"U1ABCDE92","ts":"1543240645.312600"}],
        |"unread_count":3,"ts":"1543236821.311700"},"subtype":"message_replied",
        |"hidden":true,"channel":"CYPCYPCYP","event_ts":"1543240646.312700","ts":"1543240646.312700"}
      """.stripMargin)
    val ev = json.as[MessageReplied]
    ev.message.text should be("<@U8P8P8PAG> Let me know when you are ready")
  }

  test("message blocks") {

    val json = Json.parse(Source.fromFile("src/test/resources/blocks.json").mkString)
    val ev = json.as[Seq[Block]]
    Json.toJson(ev) shouldBe json
  }

}
