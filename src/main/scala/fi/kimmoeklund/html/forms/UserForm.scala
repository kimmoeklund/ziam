package fi.kimmoeklund.html.forms

import fi.kimmoeklund.domain.{ErrorCode, NewPasswordCredentials, RoleId }
import zio.http.FormField
import zio.prelude.Validation

import java.util.UUID
import scala.util.Try
import fi.kimmoeklund.domain.FormError
import fi.kimmoeklund.html.*
import zio.prelude.Newtype
import zio.prelude.ZValidation
import fi.kimmoeklund.domain.UserId
import fi.kimmoeklund
import fi.kimmoeklund.domain.FormError.*
import fi.kimmoeklund.domain.Role
import fi.kimmoeklund.service.RoleRepository
import fi.kimmoeklund.domain.NewPassword

case class UserForm(
    @inputHidden id: Option[UserId],
    name: Option[String],
    username: Option[String],
    @inputPassword password: Option[String],
    @inputPassword password_confirmation: Option[String],
    @inputSelectOptions("roles/options", "roles", true)
    roles: Option[Set[RoleId]]
) 

object UserForm:
  given HtmlEncoder[UserForm] = HtmlEncoder.derived[UserForm]
  // todo apply macros
  def fromURLEncodedForm(
      id: Option[FormField],
      name: Option[FormField],
      username: Option[FormField],
      password: Option[FormField],
      password_confirmation: Option[FormField],
      roles: Option[FormField]
  ) = UserForm(
    id.map(_.stringValue.get).map(UUID.fromString).map(UserId(_)),
    name.map(_.stringValue.get),
    username.map(_.stringValue.get),
    password.map(_.stringValue.get),
    password_confirmation.map(_.stringValue.get),
    roles.map(_.stringValue.get.split(",").map(r => RoleId(UUID.fromString(r))).toSet)
  )
  given HtmlEncoder[Set[RoleId]] with {
    override def encodeValues(
        template: ElementTemplate,
        value: Set[RoleId],
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ) =
      HtmlEncoder[String].encodeValues(template, value.toSeq.mkString(","), errors, paramName, annotations)
    override def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any], value: Option[Set[RoleId]]) =
      HtmlEncoder[String].encodeParams(template, "roles", annotations)
  }
end UserForm

sealed trait ValidUserForm:
  val id: UserId | UUID
  val name: String
  val roles: Set[RoleId]

case class ValidUpdateUserForm(
    id: UserId,
    name: String,
    roles: Set[RoleId],
    newPassword: NewPassword 
) extends ValidUserForm:
  private def copy: Unit = ()

case class ValidNewUserForm(
    id: UUID,
    name: String,
    roles: Set[RoleId],
    credentials: NewPasswordCredentials
  ) extends ValidUserForm: 
  private def copy: Unit = ()

private def roleIdsValidation(form: UserForm): ZValidation[Nothing, FormError, Set[RoleId]] = 
    Validation
      .fromTry(
        Try(form.roles.getOrElse(Set()).map(r => RoleId(UUID.fromString(r.toString))).toSet)
      )
      .mapError(_ => ValueInvalid("roles", "unable to parse as UUID"))

object FormValidators:
  def newUser(form: UserForm): Validation[FormError, ValidNewUserForm] =
    Validation.validateWith(
      Validation.succeed(UUID.randomUUID()),
      Validation.fromOptionWith(Missing("name"))(form.name),
      roleIdsValidation(form),
      NewPasswordCredentials.fromOptions(form.username, form.password, form.password_confirmation)
    )(ValidNewUserForm.apply)

  def updateUser(form: UserForm): Validation[FormError, ValidUserForm] =
    Validation.validateWith(
      Validation.fromOptionWith(Missing("id"))(form.id),
      Validation.fromOptionWith(Missing("name"))(form.name),
      roleIdsValidation(form),
      NewPassword.fromOptions(form.password, form.password_confirmation)
    )(ValidUpdateUserForm.apply)
