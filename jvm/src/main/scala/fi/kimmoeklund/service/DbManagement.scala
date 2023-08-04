package fi.kimmoeklund.service

import zio.*
import fi.kimmoeklund.html.Site

enum DbManagementError:
  case DbAlreadyExists
  case DbDoesNotExist
  case IOError(message: String)

trait DbManagement:
  def provisionDatabase(dbName: String): IO[DbManagementError, Unit]
  def buildSites: RIO[DbManagement, Seq[Site[_]]]

object DbManagement:
  def provisionDatabase(db: String): ZIO[DbManagement, DbManagementError, Unit] =
    ZIO.serviceWithZIO(_.provisionDatabase(db))

  def buildSites: RIO[DbManagement, Seq[Site[_]]] =
    ZIO.serviceWithZIO(_.buildSites)
