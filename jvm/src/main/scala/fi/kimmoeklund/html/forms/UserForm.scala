package fi.kimmoeklund.html.forms

import fi.kimmoeklund.domain.{ErrorCode, NewPasswordCredentials, RoleId}
import zio.http.FormField
import zio.prelude.Validation

import java.util.UUID
import scala.util.Try
import fi.kimmoeklund.domain.FormError
import fi.kimmoeklund.html.*
import zio.prelude.Newtype
import zio.prelude.ZValidation

case class UserForm(
    name: Option[String],
    username: Option[String],
    @inputPassword password: Option[String],
    @inputPassword password_confirmation: Option[String],
    @inputSelectOptions("roles/options", "roles", true)
    roles: Option[Seq[RoleId]],
    @inputSelectOptions("organizations/options", "organization", false)
    organization: Option[String]
)

object UserForm:
  given HtmlEncoder[UserForm] = HtmlEncoder.derived[UserForm]
  def fromURLEncodedForm(
      name: Option[FormField],
      username: Option[FormField],
      password: Option[FormField],
      password_confirmation: Option[FormField],
      roles: Option[FormField],
      organization: Option[FormField]
  ) = UserForm(
    name.map(_.stringValue.get),
    username.map(_.stringValue.get),
    password.map(_.stringValue.get),
    password_confirmation.map(_.stringValue.get),
    roles.map(_.stringValue.get.split(",").map(r => RoleId(UUID.fromString(r)))),
    organization.map(_.stringValue.get)
  )
  given HtmlEncoder[Seq[RoleId]] with {
    override def encodeValues(template: ElementTemplate, value: Seq[RoleId], errors: Option[Seq[ErrorMsg]], paramName: Option[String], annotations: Seq[Any]) =
      HtmlEncoder[String].encodeValues(template, value.mkString(","), errors, paramName, annotations)
    override def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any]) = HtmlEncoder[String].encodeParams(template, "roles", annotations)
  }
end UserForm

case class FormWithErrors[A](
    val form: A,
    val errors: Seq[FormError]
)

case class ValidUserForm(
    name: String,
    organizationId: UUID,
    roleIds: Set[RoleId],
    credentials: NewPasswordCredentials
):
  private def copy: Unit = ()

object ValidUserForm:
  def apply(
      name: String,
      organizationId: UUID,
      roleIds: Set[RoleId],
      credentials: NewPasswordCredentials
  ): ValidUserForm = new ValidUserForm(name, organizationId, roleIds, credentials)

  def from(form: UserForm): Validation[FormError, ValidUserForm] =
    import fi.kimmoeklund.domain.FormError.*

    val orgIdValidation = for {
      uuidStr <- Validation.fromOptionWith(Missing("organization"))(
        form.organization
      )
      uuid <- Validation
        .fromTry(Try(UUID.fromString(uuidStr)))
        .mapError(_ => ValueInvalid("organization", "unable to parse as UUID"))
    } yield uuid

    val roleIdsValidation: ZValidation[Nothing, FormError, Set[RoleId]] = Validation
      .fromTry(
        Try(form.roles.getOrElse(Set()).map(r => RoleId(UUID.fromString(r.toString))).toSet)
      )
      .mapError(_ => ValueInvalid("roles", "unable to parse as UUID"))

    val newPassword = NewPasswordCredentials.fromOptions(
      form.username,
      form.password,
      form.password_confirmation
    )

    val nameValidation = Validation.fromOptionWith(Missing("name"))(form.name)
    Validation.validateWith(
      nameValidation,
      orgIdValidation,
      roleIdsValidation,
      newPassword
    )(this.apply)
