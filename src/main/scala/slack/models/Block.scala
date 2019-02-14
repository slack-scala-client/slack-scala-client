package slack.models

import play.api.libs.json._

sealed trait Block {
  val `type`: String
  val block_id: Option[String]
}

case class Divider(block_id: Option[String] = None) extends Block {
  override val `type`: String = "divider"
}

case class Section(text: TextObject, fields: Option[Seq[TextObject]],
                   accessory: Option[BlockElement], block_id: Option[String] = None) extends Block {
  override val `type`: String = "section"
}

case class ImageBlock(image_url: String, alt_text: String,
                      title: Option[PlainTextObject], block_id: Option[String] = None) extends Block {
  override val `type`: String = "image"
  require(title.forall(_.`type` == "plain_text"))
}

case class ActionsBlock(elements: Seq[BlockElement], block_id: Option[String] = None) extends Block {
  override val `type`: String = "actions"
  require(elements.size <= 5, "Maximum of 5 elements in each action block")
}

case class ContextBlock(elements: Seq[Either[ImageElement, TextObject]], block_id: Option[String] = None) extends Block {
  override val `type`: String = "context"
}

trait TextObject {
  val `type`: String
  val text: String
}

case class PlainTextObject(text: String, emoji: Option[Boolean] = None, `type`: String = "plain_text") extends TextObject

case class MarkdownTextObject(text: String, verbatim: Option[Boolean] = None, `type`: String = "mrkdwn") extends TextObject

object TextObject {
  private implicit val plainTextFmt = Json.format[PlainTextObject]
  private implicit val mrkdwnTextFmt = Json.format[MarkdownTextObject]

  private val textWrites = new Writes[TextObject] {
    def writes(text: TextObject): JsValue = {
      val json = text match {
        case t: PlainTextObject => Json.toJson(t)
        case t: MarkdownTextObject => Json.toJson(t)
      }
      Json.obj("type" -> text.`type`) ++ json.as[JsObject]
    }
  }
  private val textReads = new Reads[TextObject] {
    def reads(jsValue: JsValue): JsResult[TextObject] = {
      val value = (jsValue \ "type").as[String]
      value match {
        case "plain_text" => jsValue.validate[PlainTextObject]
        case "mrkdwn" => jsValue.validate[MarkdownTextObject]
        case other => JsError(s"Invalid text object type: $other")
      }
    }
  }

  implicit val format = Format(textReads, textWrites)
}

case class OptionObject(text: PlainTextObject, value: String)

case class OptionGroupObject(label: PlainTextObject, options: Seq[OptionObject])

case class ConfirmationObject(title: PlainTextObject, text: TextObject, confirm: PlainTextObject, deny: PlainTextObject)

trait BlockElement {
  val `type`: String
}

case class ImageElement(image_url: String, alt_text: String, `type`: String = "image") extends BlockElement {
}

case class ButtonElement(text: PlainTextObject, action_id: String, url: Option[String],
                         value: Option[String], confirm: Option[ConfirmationObject]) extends BlockElement {
  override val `type`: String = "button"
}

case class StaticSelectElement(placeholder: PlainTextObject, action_id: String, options: Seq[OptionObject],
                               option_groups: Seq[OptionGroupObject],
                               initial_option: Option[Either[OptionObject, OptionGroupObject]],
                               confirm: Option[ConfirmationObject]) extends BlockElement {
  override val `type`: String = "static_select"
}

case class ExternalSelectElement(placeholder: PlainTextObject, action_id: String, min_query_length: Option[Int],
                                 initial_option: Option[Either[OptionObject, OptionGroupObject]],
                                 confirm: Option[ConfirmationObject]) extends BlockElement {
  override val `type`: String = "external_select"
}

case class UserSelectElement(placeholder: PlainTextObject, action_id: String, initial_user: Option[String],
                             confirm: Option[ConfirmationObject]) extends BlockElement {
  override val `type`: String = "users_select"
}

case class ChannelSelectElement(placeholder: PlainTextObject, action_id: String, initial_channel: Option[String],
                                confirm: Option[ConfirmationObject]) extends BlockElement {
  override val `type`: String = "channels_select"
}

case class ConversationSelectElement(placeholder: PlainTextObject, action_id: String, initial_conversation: Option[String],
                                     confirm: Option[ConfirmationObject]) extends BlockElement {
  override val `type`: String = "conversations_select"
}

case class OverflowElement(action_id: String, options: Seq[OptionObject],
                           confirm: Option[ConfirmationObject]) extends BlockElement {
  override val `type`: String = "overflow"
}

case class DatePickerElement(action_id: String, placeholder: PlainTextObject,
                             initial_date: Option[String], confirm: Option[ConfirmationObject]) extends BlockElement {
  override val `type`: String = "datepicker"
}

object BlockElement {
  implicit val plainTextFmt = Json.format[PlainTextObject]

  implicit val optionObjFmt = Json.format[OptionObject]
  implicit val optionGrpObjFmt = Json.format[OptionGroupObject]
  implicit val confirmObjFmt = Json.format[ConfirmationObject]

  implicit val eitherOptFmt = eitherObjectFormat[OptionObject, OptionGroupObject]("text", "label")
  implicit val buttonElementFmt = Json.format[ButtonElement]
  implicit val imageElementFmt = Json.format[ImageElement]
  implicit val staticMenuElementFmt = Json.format[StaticSelectElement]
  implicit val extMenuElementFmt = Json.format[ExternalSelectElement]
  implicit val userMenuElementFmt = Json.format[UserSelectElement]
  implicit val channelMenuElementFmt = Json.format[ChannelSelectElement]
  implicit val conversationMenuElementFmt = Json.format[ConversationSelectElement]
  implicit val overflowElementFmt = Json.format[OverflowElement]
  implicit val datePickerElementFmt = Json.format[DatePickerElement]

  private val elemWrites = new Writes[BlockElement] {
    def writes(element: BlockElement): JsValue = {
      val json = element match {
        case elem: ButtonElement => Json.toJson(elem)
        case elem: ImageElement => Json.toJson(elem)
        case elem: StaticSelectElement => Json.toJson(elem)
        case elem: ExternalSelectElement => Json.toJson(elem)
        case elem: UserSelectElement => Json.toJson(elem)
        case elem: ChannelSelectElement => Json.toJson(elem)
        case elem: ConversationSelectElement => Json.toJson(elem)
        case elem: OverflowElement => Json.toJson(elem)
        case elem: DatePickerElement => Json.toJson(elem)
      }
      Json.obj("type" -> element.`type`) ++ json.as[JsObject]
    }
  }
  private val elemReads = new Reads[BlockElement] {
    def reads(jsValue: JsValue): JsResult[BlockElement] = {
      val value = (jsValue \ "type").as[String]
      value match {
        case "button" => jsValue.validate[ButtonElement]
        case "image" => jsValue.validate[ImageElement]
        case "static_select" => jsValue.validate[StaticSelectElement]
        case "external_select" => jsValue.validate[ExternalSelectElement]
        case "users_select" => jsValue.validate[UserSelectElement]
        case "conversations_select" => jsValue.validate[ConversationSelectElement]
        case "channels_select" => jsValue.validate[ChannelSelectElement]
        case "overflow" => jsValue.validate[OverflowElement]
        case "datepicker" => jsValue.validate[DatePickerElement]
        case other => JsError(s"Invalid element type: $other")
      }
    }
  }

  implicit val format = Format(elemReads, elemWrites)
}

object Block {
  implicit val plainTextFmt = Json.format[PlainTextObject]
  implicit val imageElementFmt = Json.format[ImageElement]

  implicit val eitherContextFmt = eitherObjectFormat[ImageElement, TextObject]("image_url", "text")
  implicit val dividerFmt = Json.format[Divider]
  implicit val imageBlockFmt = Json.format[ImageBlock]
  implicit val actionBlockFmt = Json.format[ActionsBlock]
  implicit val contextBlockFmt = Json.format[ContextBlock]
  implicit val sectionFmt = Json.format[Section]


  private val blockWrites = new Writes[Block] {
    def writes(block: Block): JsValue = {
      val json = block match {
        case b: Divider => Json.toJson(b)
        case b: Section => Json.toJson(b)
        case b: ImageBlock => Json.toJson(b)
        case b: ActionsBlock => Json.toJson(b)
        case b: ContextBlock => Json.toJson(b)
      }
      Json.obj("type" -> block.`type`) ++ json.as[JsObject]
    }
  }
  private val blockReads = new Reads[Block] {
    def reads(jsValue: JsValue): JsResult[Block] = {
      val value = (jsValue \ "type").as[String]
      value match {
        case "divider" => jsValue.validate[Divider]
        case "image" => jsValue.validate[ImageBlock]
        case "actions" => jsValue.validate[ActionsBlock]
        case "context" => jsValue.validate[ContextBlock]
        case "section" => jsValue.validate[Section]
        case other => JsError(s"Invalid block type: $other")
      }
    }
  }

  implicit val format = Format(blockReads, blockWrites)
}