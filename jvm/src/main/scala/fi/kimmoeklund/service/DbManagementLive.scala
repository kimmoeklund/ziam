package fi.kimmoeklund.service
import fi.kimmoeklund.html.Site
import zio.*

import java.io.FilenameFilter

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
        val rs = stmt.executeUpdate(schema)
      })
    } yield ()).mapError({ case e: Throwable =>
      DbManagementError.IOError(e.getMessage())
    })

  override def buildSites =
    (for {
      config <- ZIO.config(DbConfig.config)
      files <- ZIO.attemptBlockingIO({
        val file = new java.io.File(s"${config.dbLocation}")
        file.listFiles((dir: java.io.File, name: String) => name.endsWith(".db")).map(_.getName().replace(".db", ""))
      })
      _ <- ZIO.logInfo(s"building sites ${files.toList.map(s => s.toString)}")
    } yield files.map(Site.build(_)).toSeq)

object DbManagementLive {
  def live = ZLayer.scoped {
    for {
      dbManager <- ZIO.attempt(DbManagementLive())
    } yield (dbManager)
  }
}
