package fi.kimmoeklund.domain

type Errors = List[ErrorCode]

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

enum GetDataError extends ErrorCode {
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
