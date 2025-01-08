package fi.kimmoeklund.domain

import fi.kimmoeklund.html.forms.UserForm
import zio.json.*
import zio.prelude.{Newtype, Validation}
import zio.schema.codec

import java.util.UUID
import play.twirl.api.Html
import scala.meta.common.Convert
import scala.util.Try

sealed trait Member

enum LoginType {
  case PasswordCredentials
}

object UserId extends Newtype[UUID]:
  given Convert[UserId, String] with 
    def apply(userId: UserId): String = userId.toString
  given Convert[String, Option[UserId]] with 
    def apply(userId: String): Option[UserId] = 
      Try(java.util.UUID.fromString(userId)).toOption.map(UserId(_))
  def create: UserId = UserId(java.util.UUID.randomUUID())
type UserId = UserId.Type

case class Login(userName: String, loginType: LoginType)

case class User(id: UserId, name: String, roles: Set[Role], logins: Set[Login])
    extends Member
    with CrudResource[UserForm]:
  override val form = UserForm(
    Some(this.id),
    Some(this.name),
    this.logins.headOption.map(_.userName),
    None,
    None,
    Some(this.roles.map(_.id))
  )

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
  given Convert[Login, String] with 
    def apply(login: Login) = s"${login.userName} (${login.loginType})"
  given Convert[Set[Login], String] with 
    def apply(logins: Set[Login]) = logins.mkString(",")

object User:
  given JsonEncoder[User]   = DeriveJsonEncoder.gen[User]
  given JsonDecoder[User]   = DeriveJsonDecoder.gen[User]
  given JsonDecoder[UserId] = JsonDecoder[UUID].map(UserId(_))
  given JsonEncoder[UserId] = JsonEncoder[UUID].contramap(UserId.unwrap)
  given Convert[Login, String] with
    def apply(login: Login): String = s"${login.loginType} (${login.userName})"
  given [A](using conv: Convert[A, String]): Convert[Seq[A], String] with
    def apply(seq: Seq[A]): String = 
      seq.map(summon[Convert[A, String]].apply).mkString("<br>")
