package slack

import play.api.libs.json._

package object models {
	implicit val channelValueFmt = Json.format[ChannelValue]
	implicit val channelFmt = Json.format[Channel]
}