package fi.kimmoeklund.domain

import java.util.UUID

case class PasswordCredentials(userId: UUID, userName: String, password: String)
case class NewPasswordCredentials private (userId: UUID, userName: String, password: String, passwordConfirmation: String):
  private def copy: Unit = ()

object NewPasswordCredentials:
  def apply(userId: UUID, userName: String, password: String, passwordConfirmation: String): Option[NewPasswordCredentials] =
    if password == passwordConfirmation then Some(new NewPasswordCredentials(userId, userName, password, passwordConfirmation))
    else None



