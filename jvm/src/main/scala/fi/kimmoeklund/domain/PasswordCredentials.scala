package fi.kimmoeklund.domain

import zio.prelude.Validation

import java.util.UUID

case class PasswordCredentials(userId: UUID, userName: String, password: String)
case class NewPasswordCredentials private (userName: String, password: String):
  private def copy: Unit = ()

object NewPasswordCredentials:
  private def apply(userName: String, password: String): NewPasswordCredentials =
    new NewPasswordCredentials(userName, password)

  def fromOptions(
      userName: Option[String],
      password: Option[String],
      passwordConfirmation: Option[String]
  ): Validation[PasswordError, NewPasswordCredentials] =
    import PasswordError.*
    import Validation.*
    val passwordValidation = for {
      pdws <- validate(
        fromOption(password).mapError(_ => PasswordMissing),
        fromOption(passwordConfirmation).mapError(_ => PasswordConfirmationMissing)
      )
      pwdMatch <- if (pdws._1 == pdws._2) succeed(pdws._1) else fail(PasswordsDoNotMatch)
    } yield pwdMatch
    validateWith(fromOption(userName).mapError(_ => UserNameMissing), passwordValidation)(
      this.apply
    )

enum PasswordError extends ErrorCode:
  case PasswordMissing
  case PasswordConfirmationMissing
  case PasswordsDoNotMatch
  case UserNameMissing
