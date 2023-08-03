package fi.kimmoeklund.domain

import java.util.UUID
import zio.json._
import fi.kimmoeklund.html.{ HtmlEncoder, Identifiable }

case class Role(id: UUID, name: String, permissions: Seq[Permission]) 

object Role:
  given JsonEncoder[Role] = DeriveJsonEncoder.gen[Role]
  given JsonDecoder[Role] = DeriveJsonDecoder.gen[Role]
