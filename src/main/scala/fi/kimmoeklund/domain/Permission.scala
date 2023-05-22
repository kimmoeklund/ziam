package fi.kimmoeklund.domain
import java.util.UUID
import zio.json.*

case class Permission(id: UUID, target: String, permission: Int) extends Ordered[Permission] {
  import scala.math.Ordered.orderingToOrdered
  def compare(that: Permission): Int = this.id compare that.id
}

object Permission:
  given JsonEncoder[Permission] = DeriveJsonEncoder.gen[Permission]
  given JsonDecoder[Permission] = DeriveJsonDecoder.gen[Permission]

