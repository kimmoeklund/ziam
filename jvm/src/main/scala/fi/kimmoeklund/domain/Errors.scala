package fi.kimmoeklund.domain

type Errors = List[ErrorCode]

trait ErrorCode

enum ExceptionErrorCode extends ErrorCode {
  case Exception
}

