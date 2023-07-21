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
    import fi.kimmoeklund.domain.FormError.*

    val orgIdValidation = for {
      uuidStr <- Validation.fromOptionWith(MissingInput("organization"))(
        organizationId.flatMap(_.stringValue)
      )
      uuid <- Validation
        .fromTry(Try(UUID.fromString(uuidStr)))
        .mapError(_ => InputValueInvalid("organization", "unable to parse as UUID"))
    } yield uuid

    val roleIdsValidation = Validation
      .fromTry(
        Try(if roleIds.isEmpty then Set() else roleIds.get.stringValue.get.split(",").map(UUID.fromString).toSet)
      )
      .mapError(_ => InputValueInvalid("organization", "unable to parse as UUID"))

    val newPassword = NewPasswordCredentials.fromOptions(
      userName.flatMap(_.stringValue),
      password.flatMap(_.stringValue),
      passwordConfirmation.flatMap(_.stringValue)
    )

    val nameValidation = Validation.fromOptionWith(MissingInput("name"))(name.flatMap(_.stringValue))
    Validation.validateWith(
      nameValidation,
      orgIdValidation,
      roleIdsValidation,
      newPassword
    )(this.apply)
