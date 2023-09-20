package fi.kimmoeklund.service

import fi.kimmoeklund.service.Site
import zio.*
import java.io.FilenameFilter

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

final class DbManagementLive extends DbManagement:
  override def provisionDatabase(dbName: String): IO[DbManagementError, Unit] =
    (for {
      config <- ZIO.config(DbConfig.config)
      file <- ZIO.attemptBlockingIO({
        val file = new java.io.File(s"${config.dbLocation}/$dbName.db")
        if file.createNewFile() then ZIO.succeed(file) else ZIO.fail(DbManagementError.DbAlreadyExists)
      })
      schema <- ZIO.attempt(scala.io.Source.fromResource("ziam_schema.sql").mkString)
      _ <- ZIO.attemptBlocking({
        val ds = new org.sqlite.SQLiteDataSource()
        ds.setUrl(s"jdbc:sqlite:${config.dbLocation}/$dbName.db")
        val conn = ds.getConnection()
        val stmt = conn.createStatement()
        stmt.executeUpdate(schema)
      })
    } yield ()).mapError({ case e: Throwable =>
      DbManagementError.IOError(e.getMessage)
    })

  override def buildSites[R] =
    (for {
      config <- ZIO.config(DbConfig.config)
      files <- ZIO.attemptBlockingIO({
        val file = new java.io.File(s"${config.dbLocation}")
        file.listFiles((dir: java.io.File, name: String) => name.endsWith(".db")).map(_.getName.replace(".db", ""))
      })
      _ <- ZIO.logInfo(s"building sites ${files.toList.map(s => s.toString)}")
    } yield files.map(Site.build).toSeq)

object DbManagementLive {
  def live = ZLayer.scoped {
    for {
      dbManager <- ZIO.attempt(DbManagementLive())
    } yield (dbManager)
  }
}
