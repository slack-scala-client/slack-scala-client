package slack

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SlackUtilTest extends AnyWordSpec with Matchers {

  "SlackUtil" should {
    "extract ids properly" in {
      SlackUtil.extractMentionedIds("Hey <@buddy> it's me. Are you there?") should be(List("buddy"))
    }
  }
}
