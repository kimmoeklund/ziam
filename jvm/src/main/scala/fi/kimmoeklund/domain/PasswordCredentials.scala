package fi.kimmoeklund.domain

import zio.prelude.Validation

import java.util.UUID

case class NewPasswordCredentials private (userName: String, password: String):
  private def copy: Unit = ()

object NewPasswordCredentials:
  private def apply(userName: String, password: String): NewPasswordCredentials =
    new NewPasswordCredentials(userName, password)

  def fromOptions(
      userName: Option[String],
      password: Option[String],
      passwordConfirmation: Option[String]
  ): Validation[FormError, NewPasswordCredentials] =
    import FormError.*
    import Validation.*
    val passwordValidation = for {
      pdws <- validate(
        fromOption(password).mapError(_ => MissingInput("password")),
        fromOption(passwordConfirmation).mapError(_ => MissingInput("password-confirmation"))
      )
      pwdMatch <- if (pdws._1 == pdws._2) succeed(pdws._1) else fail(PasswordsDoNotMatch)
    } yield pwdMatch
    validateWith(fromOption(userName).mapError(_ => MissingInput("username")), passwordValidation)(
      this.apply
    )
