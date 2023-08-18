package fi.kimmoeklund.service

import fi.kimmoeklund.service.Site
import zio.*

enum DbManagementError:
  case DbAlreadyExists
  case DbDoesNotExist
  case IOError(message: String)

trait DbManagement:
  def provisionDatabase(dbName: String): IO[DbManagementError, Unit]
  def buildSites[R]: RIO[DbManagement, Seq[Site]]

object DbManagement:
  def provisionDatabase(db: String): ZIO[DbManagement, DbManagementError, Unit] =
    ZIO.serviceWithZIO(_.provisionDatabase(db))

  def buildSites: RIO[DbManagement, Seq[Site]] =
    ZIO.serviceWithZIO(_.buildSites)
