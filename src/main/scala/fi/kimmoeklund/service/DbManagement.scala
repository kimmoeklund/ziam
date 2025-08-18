package fi.kimmoeklund.service

import fi.kimmoeklund.html.pages.CookieSecret
import fi.kimmoeklund.repository.{QuillCtx, Repository}
import fi.kimmoeklund.html.Site
import zio.*

import java.io.FilenameFilter

enum DbManagementError:
  case DbAlreadyExists
  case DbDoesNotExist
  case IOError(message: String)

type Env = Map[String, QuillCtx] & Repository[?, ?, ?]

trait DbManagement:
  def provisionDatabase(dbName: String): IO[DbManagementError, Unit]
  def getDatabases: RIO[DbManagement, Seq[String]]

object DbManagement:
  def provisionDatabase(db: String): ZIO[DbManagement, DbManagementError, Unit] =
    ZIO.serviceWithZIO(_.provisionDatabase(db))

  def getDatabases: RIO[DbManagement, Seq[String]] =
    ZIO.serviceWithZIO(_.getDatabases)

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

  override def getDatabases: ZIO[DbManagement, Exception, Seq[String]] = for {
    config <- ZIO.config(DbConfig.config)
    files <- ZIO.attemptBlockingIO({
      val file = java.io.File(s"${config.dbLocation}")
      file.listFiles((dir: java.io.File, name: String) => name.endsWith(".db")).map(_.getName.replace(".db", ""))
    })
    _ <- ZIO.log(files.mkString(","))
  } yield files.toSeq

object DbManagementLive {
  def live = ZLayer.scoped {
    for {
      cookieSecret <- ZIO.config(Secrets.cookieSecret)
      dbManager    <- ZIO.attempt(DbManagementLive(cookieSecret))
    } yield (dbManager)
  }
}
