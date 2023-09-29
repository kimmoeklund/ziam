package fi.kimmoeklund.domain

import fi.kimmoeklund.html.{HtmlEncoder, Identifiable}
import zio.json.*
import zio.prelude.Validation
import zio.schema.codec

import java.util.UUID
import zio.http.html.Template
import fi.kimmoeklund.html.ElementTemplate
import fi.kimmoeklund.html.ErrorMsg
import zio.prelude.Newtype
import zio.http.html.Html
import fi.kimmoeklund.html.forms.UserForm

sealed trait Member

enum LoginType {
  case PasswordCredentials
}

object UserId extends Newtype[UUID]:
  given HtmlEncoder[UserId] with {
    override def encodeValues(
        template: ElementTemplate,
        value: UserId,
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ): List[Html] = HtmlEncoder[String].encodeValues(template, value.toString(), errors, paramName, annotations)
    override def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any], value: Option[UserId]) =
      HtmlEncoder[String].encodeParams(template, "id", annotations)
  }
  given HtmlEncoder[Seq[UserId]] with {
    override def encodeValues(
        template: ElementTemplate,
        value: Seq[UserId],
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ) =
      HtmlEncoder[String].encodeValues(template, value.map(UserId.unwrap).mkString(","), errors, paramName, annotations)
    override def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any], value: Option[Seq[UserId]]) =
      HtmlEncoder[UUID].encodeParams(template, "ids")
  }

type UserId = UserId.Type

case class Login(userName: String, loginType: LoginType)

case class User(id: UserId, name: String, roles: Set[Role], logins: Seq[Login]) extends Member with CrudResource[User, UserForm]:
  override def form = UserForm(Some(this.id), Some(this.name),  this.logins.headOption.map(_.userName), None, None, Some(this.roles.map(_.id)))
  override def resource = this

case class NewPasswordUser(
    id: UserId,
    name: String,
    credentials: NewPasswordCredentials,
    roles: Set[Role]
)

object NewPasswordUser:
  def fromArguments(
      id: UUID,
      name: String,
      userName: String,
      password: String,
      roles: Set[Role] = Set()
  ) =
    new NewPasswordUser(
      UserId(id),
      name,
      NewPasswordCredentials(userName, password),
      roles
    )

object Login:
  given JsonEncoder[LoginType] = DeriveJsonEncoder.gen[LoginType]
  given JsonDecoder[LoginType] = DeriveJsonDecoder.gen[LoginType]
  given JsonEncoder[Login]     = DeriveJsonEncoder.gen[Login]
  given JsonDecoder[Login]     = DeriveJsonDecoder.gen[Login]
  given [A: HtmlEncoder]: HtmlEncoder[Seq[Login]] with {
    override def encodeValues(
        template: ElementTemplate,
        value: Seq[Login],
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ) =
      HtmlEncoder[String].encodeValues(template, value.map(v => s"${v.loginType} (${v.userName})").mkString("<br>"))
    override def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any], value: Option[Seq[Login]]) =
      HtmlEncoder[String].encodeParams(template, "logins")
  }
  given HtmlEncoder[Login]     = HtmlEncoder.derived[Login]
  given HtmlEncoder[LoginType] = HtmlEncoder.derived[LoginType]

object User:
  given JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  given JsonDecoder[User] = DeriveJsonDecoder.gen[User]
  given JsonDecoder[UserId] = JsonDecoder[UUID].map(UserId(_))
  given JsonEncoder[UserId] = JsonEncoder[UUID].contramap(UserId.unwrap)
