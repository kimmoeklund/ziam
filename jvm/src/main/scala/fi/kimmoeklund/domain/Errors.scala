package fi.kimmoeklund.domain

type Errors = List[ErrorCode]

trait ErrorCode

enum GeneralErrors extends ErrorCode {
  case Exception
  case EntityNotFound(id: String)
  case UniqueKeyViolation(details: String)
}

