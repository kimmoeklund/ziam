package fi.kimmoeklund.domain

import fi.kimmoeklund.html.{HtmlEncoder, Identifiable}
import zio.json._

import java.util.UUID
import fi.kimmoeklund.html.ElementTemplate
import fi.kimmoeklund.html.ErrorMsg
import zio.prelude.Newtype
import zio.http.html.Html

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
    override def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any]) =
      HtmlEncoder[String].encodeParams(template, "role", annotations)
  }
  given HtmlEncoder[Seq[RoleId]] with {
    override def encodeValues(
        template: ElementTemplate,
        value: Seq[RoleId],
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ) =
      HtmlEncoder[String].encodeValues(template, value.mkString(","), errors, paramName, annotations)
    override def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any]) =
      HtmlEncoder[String].encodeParams(template, "roles", annotations)
  }
end RoleId

type RoleId = RoleId.Type
case class Role(id: RoleId, name: String, permissions: Seq[Permission]) derives HtmlEncoder

object Role:
  given JsonEncoder[Role] = DeriveJsonEncoder.gen[Role]
  given JsonDecoder[Role] = DeriveJsonDecoder.gen[Role]
  given JsonDecoder[RoleId] = JsonDecoder[UUID].map(RoleId(_))
  given JsonEncoder[RoleId] = JsonEncoder[UUID].contramap(RoleId.unwrap)
  given HtmlEncoder[Seq[Role]] with {
    override def encodeValues(
        template: ElementTemplate,
        value: Seq[Role],
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
    override def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any]) =
      HtmlEncoder[String].encodeParams(template, "roles")
  }
