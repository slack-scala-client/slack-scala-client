package slack.models

case class Attachment (
  fallback: Option[String] = None,
  color: Option[String] = None,
  pretext: Option[String] = None,
  author_name: Option[String] = None,
  author_link: Option[String] = None,
  author_icon: Option[String] = None,
  title: Option[String] = None,
  title_link: Option[String] = None,
  text: Option[String] = None,
  fields: Seq[AttachmentField] = Seq[AttachmentField](),
  image_url: Option[String] = None,
  thumb_url: Option[String] = None
)

case class AttachmentField (
  title: String,
  value: String,
  short: Boolean
)