package fi.kimmoeklund.service
import zio.*

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
      _ <- ZIO.logInfo("schema created")
    } yield ()).mapError({ case e: Throwable =>
      DbManagementError.IOError
    })

object DbManagementLive {
  def live = ZLayer.scoped {
    for {
      dbManager <- ZIO.attempt(DbManagementLive())
    } yield (dbManager)
  }
}
