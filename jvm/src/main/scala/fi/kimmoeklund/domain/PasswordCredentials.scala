package fi.kimmoeklund.domain

import zio.prelude.Validation

case class NewPasswordCredentials(userName: String, password: String)

object NewPasswordCredentials:
  def apply(userName: String, password: String): NewPasswordCredentials =
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
        fromOption(password).mapError(_ => Missing("password")),
        fromOption(passwordConfirmation).mapError(_ => Missing("password_confirmation"))
      )
      pwdMatch <- if (pdws._1 == pdws._2) succeed(pdws._1) else fail(PasswordsDoNotMatch)
    } yield pwdMatch
    validateWith(fromOption(userName).mapError(_ => Missing("username")), passwordValidation)(
      this.apply
    )
