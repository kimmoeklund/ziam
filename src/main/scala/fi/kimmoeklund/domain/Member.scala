package fi.kimmoeklund.domain

import java.util.UUID
import zio.json._

sealed trait Member

case class Group(id: UUID, name: String) extends Member
case class User(id: UUID, name: String, roles: Seq[Role]) extends Member
case class Organization(id: UUID, name: String) extends Member
 
object Member:
  given JsonEncoder[Member] = DeriveJsonEncoder.gen[Member]
  given JsonDecoder[Member] = DeriveJsonDecoder.gen[Member]

object User:
  given JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  given JsonDecoder[User] = DeriveJsonDecoder.gen[User]

