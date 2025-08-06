package fi.kimmoeklund.html

import fi.kimmoeklund.domain.FormError.ValueInvalid
import fi.kimmoeklund.domain.{ErrorCode, ExistingEntityError, FormError, InsertDataError}
import fi.kimmoeklund.html.encoder.ErrorMsg

case class DefaultErrorHandler(resource: String):

  def handle(error: ErrorCode): ErrorMsg = error match {
    case e: ExistingEntityError => mapFormError(mapExistingEntityError(e, "resource"))
    case e: InsertDataError     => mapFormError(mapInsertDataError(e))
    case _                      => mapFormError(error)
  }

  def mapExistingEntityError(error: ExistingEntityError, resource: String): FormError = error match {
    case a: ExistingEntityError.EntityNotFound[_] =>
      FormError.ValueInvalid(resource, s"Requested $resource was not found, please reload the page and try again.")
    case _ =>
      FormError.ProcessingFailed(s"System failure while creating user. User was not created, please try again later.")
  }

  def mapInsertDataError(error: InsertDataError): FormError = error match {
    case InsertDataError.UniqueKeyViolation("userName") =>
      FormError.ValueInvalid("username", "Username is already taken, please select another one.")
    case _ =>
      FormError.ProcessingFailed(
        s"System failure while creating $resource. ${resource.capitalize} was not created, please try again later."
      )
  }

  def mapFormError(error: ErrorCode) = error match {
    case FormError.Missing(field) => ErrorMsg(field, s"$field is mandatory")
    case FormError.PasswordsDoNotMatch =>
      ErrorMsg("password_confirmation", "password confirmation does not match")
    case ValueInvalid(field, details) => ErrorMsg(field, details)
    case _                            => ErrorMsg("", "System error processing the form")
  }
