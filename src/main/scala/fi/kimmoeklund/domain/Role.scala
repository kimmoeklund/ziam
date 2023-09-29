package fi.kimmoeklund.domain

import fi.kimmoeklund.html.{HtmlEncoder, Identifiable}
import zio.json._

import java.util.UUID
import fi.kimmoeklund.html.ElementTemplate
import fi.kimmoeklund.html.ErrorMsg
import zio.prelude.Newtype
import zio.http.html.Html
import fi.kimmoeklund.html.pages.RoleForm

object RoleId extends Newtype[UUID]:
  given HtmlEncoder[RoleId] with {
    override def encodeValues(
        template: ElementTemplate,
        value: RoleId,
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ) =
      HtmlEncoder[String].encodeValues(template, value.toString, errors, paramName, annotations)
    override def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any], value: Option[RoleId]) =
      HtmlEncoder[String].encodeParams(template, "role", annotations, value.map(RoleId.unwrap(_).toString))
  }
  given HtmlEncoder[Set[RoleId]] with {
    override def encodeValues(
        template: ElementTemplate,
        value: Set[RoleId],
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ) =
      HtmlEncoder[String].encodeValues(template, value.toSeq.mkString(","), errors, paramName, annotations)
    override def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any], value: Option[Set[RoleId]]) =
      HtmlEncoder[String].encodeParams(template, "roles", annotations)
  }
end RoleId

type RoleId = RoleId.Type

case class Role(id: RoleId, name: String, permissions: Seq[Permission]) extends CrudResource[Role, RoleForm] derives HtmlEncoder:
  override def form = RoleForm(this.name, this.permissions.map(_.id.toString))
  override def resource = this

object Role:
  given JsonEncoder[Role] = DeriveJsonEncoder.gen[Role]
  given JsonDecoder[Role] = DeriveJsonDecoder.gen[Role]
  given JsonDecoder[RoleId] = JsonDecoder[UUID].map(RoleId(_))
  given JsonEncoder[RoleId] = JsonEncoder[UUID].contramap(RoleId.unwrap)
  given HtmlEncoder[Set[Role]] with {
    override def encodeValues(
        template: ElementTemplate,
        value: Set[Role],
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ) =
      HtmlEncoder[String].encodeValues(
        template,
        value.map(v => s"${v.name}").mkString("<br>"),
        errors,
        paramName,
        annotations
      )
    override def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any], value: Option[Set[Role]]) =
      HtmlEncoder[String].encodeParams(template, "roles")
  }
