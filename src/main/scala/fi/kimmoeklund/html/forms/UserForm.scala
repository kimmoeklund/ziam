package fi.kimmoeklund.html.forms

import fi.kimmoeklund.domain.{ErrorCode, NewPasswordCredentials, RoleId }
import zio.http.FormField
import zio.prelude.Validation

import scala.util.Try
import fi.kimmoeklund.domain.FormError
import fi.kimmoeklund.html.*
import zio.prelude.Newtype
import zio.prelude.ZValidation
import fi.kimmoeklund.domain.UserId
import fi.kimmoeklund
import fi.kimmoeklund.domain.FormError.*
import fi.kimmoeklund.domain.Role
import fi.kimmoeklund.domain.NewPassword

case class UserForm(
    @inputHidden id: Option[UserId],
    name: Option[String],
    username: Option[String],
    @inputPassword password: Option[String],
    @inputPassword password_confirmation: Option[String],
    @inputSelectOptions(s"/page/roles/options", "roles", true)
    roles: Option[Set[RoleId]]
) 

sealed trait ValidUserForm:
  val id: UserId
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
    id: UserId,
    name: String,
    roles: Set[RoleId],
    credentials: NewPasswordCredentials
  ) extends ValidUserForm: 
  private def copy: Unit = ()

private def roleIdsValidation(form: UserForm): ZValidation[Nothing, FormError, Set[RoleId]] = form.roles match {
  case Some(r) => Validation.succeed(r)
  case None => Validation.fail(ValueInvalid("roles", "unable to parse as UUID"))
}

object FormValidators:
  def newUser(form: UserForm): Validation[FormError, ValidNewUserForm] =
    Validation.validateWith(
      Validation.succeed(UserId.create),
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
