package fi.kimmoeklund.domain

import fi.kimmoeklund.html.{HtmlEncoder, Identifiable}
import zio.json.*
import zio.prelude.Validation
import zio.schema.codec

import java.util.UUID
import zio.http.html.Template
import fi.kimmoeklund.html.ElementTemplate
import fi.kimmoeklund.html.ErrorMsg

sealed trait Member

enum LoginType {
  case PasswordCredentials
}

type UserId = UUID
type OrganizationId = UUID

case class Login(userName: String, loginType: LoginType)

case class User(id: UUID, name: String, organization: Organization, roles: Seq[Role], logins: Seq[Login]) extends Member
case class Organization(id: UUID, name: String) extends Member with Identifiable
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
  given [A: HtmlEncoder]: HtmlEncoder[Seq[Login]] with {
    override def encodeValues(template: ElementTemplate, value: Seq[Login], errors: Option[Seq[ErrorMsg]], paramName: Option[String], annotations: Seq[Any]) =
      HtmlEncoder[String].encodeValues(template, value.map(v => s"${v.loginType} (${v.userName})").mkString("<br>"))
    override def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any]) = HtmlEncoder[String].encodeParams(template, "logins")
  }
  given HtmlEncoder[Login] = HtmlEncoder.derived[Login]
  given HtmlEncoder[LoginType] = HtmlEncoder.derived[LoginType]
object Member:
  given JsonEncoder[Member] = DeriveJsonEncoder.gen[Member]
  given JsonDecoder[Member] = DeriveJsonDecoder.gen[Member]

object User:
  given JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  given JsonDecoder[User] = DeriveJsonDecoder.gen[User]

object Organization:
  given JsonEncoder[Organization] = DeriveJsonEncoder.gen[Organization]
  given JsonDecoder[Organization] = DeriveJsonDecoder.gen[Organization]
  given HtmlEncoder[Organization] = HtmlEncoder.derived[Organization]
