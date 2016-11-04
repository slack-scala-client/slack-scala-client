package slack.models

case class Attachment (
  fallback: Option[String] = None,
  callback_id: Option[String] = None,
  color: Option[String] = None,
  pretext: Option[String] = None,
  author_name: Option[String] = None,
  author_link: Option[String] = None,
  author_icon: Option[String] = None,
  title: Option[String] = None,
  title_link: Option[String] = None,
  text: Option[String] = None,
  fields: Seq[AttachmentField] = Seq.empty,
  image_url: Option[String] = None,
  thumb_url: Option[String] = None,
  actions: Seq[ActionField] = Seq.empty,
  mrkdwn_in: Seq[String] = Seq.empty
)

case class AttachmentField(title: String, value: String, short: Boolean)

case class ActionField(name: String,
                       text: String, `type`: String,
                       style: Option[String] = None,
                       value: Option[String] = None, confirm: Option[ConfirmField] = None)

case class ConfirmField(text: String, title: Option[String] = None,
                        ok_text: Option[String] = None, cancel_text: Option[String] = None)
