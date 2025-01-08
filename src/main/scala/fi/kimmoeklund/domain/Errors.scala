package fi.kimmoeklund.domain

case class FormWithErrors[A](errors: List[ErrorCode], form: Option[A])

sealed trait ErrorCode

sealed trait FieldError:
  val fieldName: String

enum GeneralError extends ErrorCode {
  case Exception(e: String)
  case EntityNotFound[A](id: String)
  case UniqueKeyViolation(fieldName: String)
  case IncorrectPassword
  case DbNotFound
  case PageNotFound
}

enum ExistingEntityError extends ErrorCode {
  case Exception(e: String)
  case EntityNotFound[A](id: String)
}

enum InsertDataError extends ErrorCode {
  case Exception(e: String)
  case UniqueKeyViolation(details: String)
}

enum FormError extends ErrorCode {
  case Missing(fieldName: String)
  case PasswordsDoNotMatch
  case ValueInvalid(fieldName: String, details: String)
  case ProcessingFailed(details: String)
}
