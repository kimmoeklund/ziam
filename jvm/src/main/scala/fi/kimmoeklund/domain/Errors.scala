package fi.kimmoeklund.domain

type Errors = List[ErrorCode]

sealed trait ErrorCode

enum GeneralErrors extends ErrorCode {
  case Exception
  case EntityNotFound[A](id: String)
  case UniqueKeyViolation(details: String)
}

enum FormError extends ErrorCode {
  case MissingInput(fieldName: String)
  case PasswordsDoNotMatch
  case InputValueInvalid(fieldName: String, details: String)
}
