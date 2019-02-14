package slack.models

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
                      title: Option[TextObject], block_id: Option[String] = None) extends Block {
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

case class TextObject(`type`: String, text: String, emoji: Option[Boolean], verbatim: Option[Boolean])

case class OptionObject(text: TextObject, value: String)

case class OptionGroupObject(label: TextObject, options: Seq[OptionObject])

case class ConfirmationObject(title: TextObject, text: TextObject, confirm: TextObject, deny: TextObject)

trait BlockElement {
  val `type`: String
}

case class ImageElement(image_url: String, alt_text: String) extends BlockElement {
  override val `type`: String = "image"
}

case class ButtonElement(text: String, action_id: String, url: Option[String],
                         value: Option[String], confirm: Option[ConfirmationObject]) extends BlockElement {
  override val `type`: String = "button"
}

case class MenuElement(placeholder: TextElement, action_id: String, options: Seq[OptionObject],
                       option_groups: Seq[OptionGroupObject],
                       initial_option: Option[Either[OptionObject, OptionGroupObject]],
                       confirm: Option[ConfirmationObject]) extends BlockElement {
  override val `type`: String = "static_select"
}

case class OverflowElement(action_id: String, options: Seq[OptionObject],
                           confirm: Option[ConfirmationObject]) extends BlockElement {
  override val `type`: String = "overflow"
}

case class DatePickerElement(action_id: String, placeholder: TextObject,
                             initial_date: Option[String], confirm: Option[ConfirmationObject]) extends BlockElement {
  override val `type`: String = "datepicker"
}