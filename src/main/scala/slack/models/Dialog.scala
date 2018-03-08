package slack.models

case class Dialog(
                   callback_id: String,
                   title: String,
                   submit_label: String,
                   elements: Seq[DialogElement])

trait DialogElement {
  val `type`: String
  val label: String
  val name: String
  val placeholder: Option[String]
  val value: Option[String]
  val optional: Boolean
}

case class TextElement(label: String,
                       name: String,
                       optional: Boolean = false,
                       max_length: Option[Int] = None,
                       min_length: Option[Int] = None,
                       hint: Option[String] = None,
                       subtype: Option[String] = None,
                       placeholder: Option[String] = None,
                       value: Option[String] = None,
                       `type`: String = "text") extends DialogElement

case class SelectElement(label: String,
                         name: String,
                         options: Seq[OptionElement],
                         optional: Boolean = false,
                         placeholder: Option[String] = None,
                         value: Option[String] = None,
                         `type`: String = "select") extends DialogElement

case class OptionElement(label: String, value: String)
