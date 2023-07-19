package fi.kimmoeklund.html.forms

import fi.kimmoeklund.domain.{ErrorCode, NewPasswordCredentials}
import zio.http.FormField
import zio.prelude.Validation

import java.util.UUID
import scala.util.Try

case class NewUserForm(
    name: String,
    organizationId: UUID,
    roleIds: Set[UUID],
    credentials: NewPasswordCredentials
):
  private def copy: Unit = ()

enum NewUserFormErrors extends ErrorCode:
  case NameMissing
  case OrganizationIdMissing
  case OrganizationIdInvalid
  case OrganizationNotFound
  case RoleIdsInvalid

object NewUserForm:
  def apply(
      name: String,
      organizationId: UUID,
      roleIds: Set[UUID],
      credentials: NewPasswordCredentials
  ): NewUserForm = new NewUserForm(name, organizationId, roleIds, credentials)

  def fromOptions(
      name: Option[FormField],
      organizationId: Option[FormField],
      roleIds: Option[FormField],
      userName: Option[FormField],
      password: Option[FormField],
      passwordConfirmation: Option[FormField]
  ): Validation[ErrorCode, NewUserForm] =
    import NewUserFormErrors.*

    val orgIdValidation = for {
      uuidStr <- Validation.fromOptionWith(OrganizationIdMissing)(
        organizationId.flatMap(_.stringValue)
      )
      uuid <- Validation.fromTry(Try(UUID.fromString(uuidStr))).mapError(_ => OrganizationIdInvalid)
    } yield uuid

    val roleIdsValidation = Validation
      .fromTry(
        Try(if roleIds.isEmpty then Set() else roleIds.get.stringValue.get.split(",").map(UUID.fromString).toSet)
      )
      .mapError(_ => RoleIdsInvalid)

    val newPassword = NewPasswordCredentials.fromOptions(
      userName.flatMap(_.stringValue),
      password.flatMap(_.stringValue),
      passwordConfirmation.flatMap(_.stringValue)
    )

    val nameValidation = Validation.fromOptionWith(NameMissing)(name.flatMap(_.stringValue))
    Validation.validateWith(
      nameValidation,
      orgIdValidation,
      roleIdsValidation,
      newPassword
    )(this.apply)

//    NewUserForm(
//      Validation.succeed(name),
//      Validation.succeed(organizationId),
//      Validation.succeed(roleIds),
//      Validation.succeed(credentials)
//    )
