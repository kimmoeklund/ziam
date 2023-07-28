package fi.kimmoeklund.service

import zio.*

enum DbManagementError:
  case DbAlreadyExists
  case DbDoesNotExist
  case IOError

trait DbManagement:
  def provisionDatabase(dbName: String): IO[DbManagementError, Unit]

object DbManagement:
  def provisionDatabase(db: String): ZIO[DbManagement, DbManagementError, Unit] =
    ZIO.serviceWithZIO(_.provisionDatabase(db))
