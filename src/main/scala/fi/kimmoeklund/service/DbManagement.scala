package fi.kimmoeklund.service

import fi.kimmoeklund.service.Site
import zio.*
import java.io.FilenameFilter
import fi.kimmoeklund.html.pages.CookieSecret
import fi.kimmoeklund.html.Page

enum DbManagementError:
  case DbAlreadyExists
  case DbDoesNotExist
  case IOError(message: String)

trait DbManagement:
  def provisionDatabase(dbName: String): IO[DbManagementError, Unit]
  def buildSites[R <: Repositories]: RIO[DbManagement, Seq[Site[R]]]

object DbManagement:
  def provisionDatabase[R <: Repositories](db: String): ZIO[DbManagement, DbManagementError, Unit] =
    ZIO.serviceWithZIO(_.provisionDatabase(db))

  def buildSites[R <: Repositories](): RIO[DbManagement, Seq[Site[R]]] =
   ZIO.serviceWithZIO(_.buildSites)

final class DbManagementLive(val cookieSecret: CookieSecret) extends DbManagement:
  override def provisionDatabase(dbName: String): IO[DbManagementError, Unit] =
    (for {
      config <- ZIO.config(DbConfig.config)
      file <- ZIO.attemptBlockingIO({
        val file = java.io.File(s"${config.dbLocation}/$dbName.db")
        if file.createNewFile() then ZIO.succeed(file) else ZIO.fail(DbManagementError.DbAlreadyExists)
      })
      schema <- ZIO.attempt(scala.io.Source.fromResource("ziam_schema.sql").mkString)
      _ <- ZIO.attemptBlockingIO({
        val ds = org.sqlite.SQLiteDataSource()
        ds.setUrl(s"jdbc:sqlite:${config.dbLocation}/$dbName.db")
        val conn = ds.getConnection()
        val stmt = conn.createStatement()
        stmt.executeUpdate(schema)
      })
    } yield ()).mapError({ case e: Throwable =>
      DbManagementError.IOError(e.getMessage)
    })

  override def buildSites[R <: Repositories] =
    (for {
      config <- ZIO.config(DbConfig.config)
      files <- ZIO.attemptBlockingIO({
        val file = java.io.File(s"${config.dbLocation}")
        file.listFiles((dir: java.io.File, name: String) => name.endsWith(".db")).map(_.getName.replace(".db", ""))
      })
      _ <- ZIO.logInfo(s"building sites ${files.toList.map(s => s.toString)}")
    } yield files.map(db => Site.build[R](db, cookieSecret)).toSeq)

object DbManagementLive {
  def live = ZLayer.scoped {
    for {
      cookieSecret <- ZIO.config(Secrets.cookieSecret)
      dbManager <- ZIO.attempt(DbManagementLive(cookieSecret))
    } yield (dbManager)
  }
}
