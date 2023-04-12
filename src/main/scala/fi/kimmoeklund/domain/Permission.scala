package fi.kimmoeklund.domain
import java.util.UUID
import zio.json.*

case class Permission(id: UUID, target: String, permission: Int)

object Permission:
  given JsonEncoder[Permission] = DeriveJsonEncoder.gen[Permission]
  given JsonDecoder[Permission] = DeriveJsonDecoder.gen[Permission]
