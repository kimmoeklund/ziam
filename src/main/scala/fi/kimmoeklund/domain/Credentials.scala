package fi.kimmoeklund.domain

import zio.prelude.Validation
import zio.prelude.Newtype
import java.util.UUID
import FormError.*
import Validation.*

case class NewPasswordCredentials(userName: String, password: String)
case class NewPassword(password: String)

def matchEmptyToNone(s: Option[String]): Option[String] = if (s.isDefined && s.get == "") None else s

def passwordsMustMatch(password: Option[String], passwordConfirmation: Option[String]) =
  for {
    pdws <- validate(
      fromOption(matchEmptyToNone(password)).mapError(_ => Missing("password")),
      fromOption(matchEmptyToNone(passwordConfirmation)).mapError(_ => Missing("password_confirmation"))
    )
    pwdMatch <- if (pdws._1 == pdws._2) succeed(pdws._1) else fail(PasswordsDoNotMatch)
  } yield pwdMatch

object NewPasswordCredentials:
  def fromOptions(
      userName: Option[String],
      password: Option[String],
      passwordConfirmation: Option[String]
  ): Validation[FormError, NewPasswordCredentials] =
    val passwordValidation = passwordsMustMatch(password, passwordConfirmation)
    validateWith(fromOption(matchEmptyToNone(userName)).mapError(_ => Missing("username")), passwordValidation)(
      NewPasswordCredentials.apply
    )

object NewPassword:
  def fromOptions(password: Option[String], passwordConfirmation: Option[String]): Validation[FormError, NewPassword] =
    passwordsMustMatch(password, passwordConfirmation).map(NewPassword.apply)

case class OAuthCredentials(memberId: UUID, email: Email, iss: String)

object Email extends Newtype[String]
type Email = Email.Type
