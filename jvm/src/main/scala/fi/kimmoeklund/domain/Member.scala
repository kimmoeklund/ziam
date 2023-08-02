package fi.kimmoeklund.domain

import zio.json.*
import zio.prelude.Validation
import zio.schema.codec

import java.util.UUID
import fi.kimmoeklund.html.ZiamHtml

sealed trait Member

enum LoginType {
  case PasswordCredentials
}

given ZiamHtml[LoginType] = (s, e) => List(e(s.toString))

type UserId = UUID
type OrganizationId = UUID

case class Login(userName: String, loginType: LoginType) derives ZiamHtml

case class User(id: UUID, name: String, organization: Organization, roles: Seq[Role], logins: Seq[Login]) extends Member
    derives ZiamHtml
case class Organization(id: UUID, name: String) extends Member derives ZiamHtml
case class NewPasswordUser(
    id: UserId,
    name: String,
    organization: Organization,
    credentials: NewPasswordCredentials,
    roles: Seq[Role]
)

object NewPasswordUser:
  def fromArguments(
      name: String,
      userName: String,
      password: String,
      organization: Organization,
      roles: Seq[Role] = Seq()
  ) =
    new NewPasswordUser(
      UUID.randomUUID(),
      name,
      organization,
      NewPasswordCredentials(userName, password),
      roles
    )

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
