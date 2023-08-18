package fi.kimmoeklund.domain

import fi.kimmoeklund.html.{HtmlEncoder, Identifiable}
import zio.json.*

import java.util.UUID
import fi.kimmoeklund.html.ElementTemplate
import fi.kimmoeklund.html.ErrorMsg

case class Permission(id: UUID, target: String, permission: Int) extends Identifiable with Ordered[Permission] {
  import scala.math.Ordered.orderingToOrdered
  def compare(that: Permission): Int = this.id compare that.id
}

object Permission:
  given JsonEncoder[Permission] = DeriveJsonEncoder.gen[Permission]
  given JsonDecoder[Permission] = DeriveJsonDecoder.gen[Permission]
  given HtmlEncoder[Permission] = HtmlEncoder.derived[Permission]
  given HtmlEncoder[Seq[Permission]] with {
    override def encodeValues(
        template: ElementTemplate,
        value: Seq[Permission],
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ) =
      HtmlEncoder[String].encodeValues(template, value.map(v => s"${v.target} (${v.permission})").mkString("<br>"))
    override def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any]) =
      HtmlEncoder[String].encodeParams(template, "permissions")
  }
