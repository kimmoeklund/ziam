package fi.kimmoeklund.domain

type Errors = List[ErrorCode]

sealed trait ErrorCode

enum GeneralErrors extends ErrorCode {
  case Exception(e: String)
  case EntityNotFound[A](id: String)
  case UniqueKeyViolation(details: String)
  case IncorrectPassword
  case DbNotFound
  case PageNotFound
}

enum FormError extends ErrorCode {
  case MissingInput(fieldName: String)
  case PasswordsDoNotMatch
  case InputValueInvalid(fieldName: String, details: String)
}
