package fi.kimmoeklund.domain

import java.util.UUID
import zio.json.*
import fi.kimmoeklund.html.{ HtmlEncoder, Identifiable }

case class Permission(id: UUID, target: String, permission: Int) extends Identifiable with Ordered[Permission] {
  import scala.math.Ordered.orderingToOrdered
  def compare(that: Permission): Int = this.id compare that.id
} 

object Permission:
  given JsonEncoder[Permission] = DeriveJsonEncoder.gen[Permission]
  given JsonDecoder[Permission] = DeriveJsonDecoder.gen[Permission]
  given HtmlEncoder[Permission] = HtmlEncoder.derived[Permission]


