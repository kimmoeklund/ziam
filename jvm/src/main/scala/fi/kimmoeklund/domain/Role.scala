package fi.kimmoeklund.domain

import fi.kimmoeklund.html.{HtmlEncoder, Identifiable}
import zio.json._

import java.util.UUID

case class Role(id: UUID, name: String, permissions: Seq[Permission])

object Role:
  given JsonEncoder[Role] = DeriveJsonEncoder.gen[Role]
  given JsonDecoder[Role] = DeriveJsonDecoder.gen[Role]
