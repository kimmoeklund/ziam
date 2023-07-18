package fi.kimmoeklund.domain

import java.util.UUID
import zio.json.*
import zio.schema.codec

sealed trait Member

enum LoginType {
  case PasswordCredentials
}

case class Login(userName: String, loginType: LoginType)

case class Group(id: UUID, name: String) extends Member
case class User(id: UUID, name: String, organization: Organization, roles: Seq[Role], logins: Seq[Login]) extends Member
case class Organization(id: UUID, name: String) extends Member
case class NewPasswordUser(id: UUID, name: String, organization: Organization, credentials: NewPasswordCredentials, roles: Seq[Role])

object Login:
  given JsonEncoder[LoginType] = DeriveJsonEncoder.gen[LoginType]
  given JsonDecoder[LoginType] = DeriveJsonDecoder.gen[LoginType]
  given JsonEncoder[Login] = DeriveJsonEncoder.gen[Login]
  given JsonDecoder[Login] = DeriveJsonDecoder.gen[Login]

object Member:
  given JsonEncoder[Member] = DeriveJsonEncoder.gen[Member]
  given JsonDecoder[Member] = DeriveJsonDecoder.gen[Member]

object User:
  given JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  given JsonDecoder[User] = DeriveJsonDecoder.gen[User]

object Organization:
  given JsonEncoder[Organization] = DeriveJsonEncoder.gen[Organization]
  given JsonDecoder[Organization] = DeriveJsonDecoder.gen[Organization]
