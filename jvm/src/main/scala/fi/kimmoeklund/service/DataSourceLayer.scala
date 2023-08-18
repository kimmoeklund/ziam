package fi.kimmoeklund.service

import io.getquill.jdbczio.Quill
import io.getquill.jdbczio.Quill.*
import io.getquill.{CompositeNamingStrategy2, Escape, NamingStrategy, SnakeCase}
import org.sqlite.SQLiteDataSource
import zio.*

import javax.sql.DataSource

object DataSourceLayer:

  def sqlite(keys: Seq[String]) = {
    val dataSource = ZIO
      .foreach(keys) { key =>
        for {
          config <- ZIO.config(DbConfig.config)
          ds <- ZIO.succeed({
            val ds = SQLiteDataSource()
            ds.setUrl(s"jdbc:sqlite:${config.dbLocation}/${key}.db")
            ds.asInstanceOf[DataSource]
          })
        } yield (key, ds)
      }
      .map(t => ZEnvironment(t.toMap))
    ZLayer.fromZIOEnvironment(dataSource)
  }

  def quill(
      paths: Seq[String]
  ): ZLayer[
    Map[String, DataSource],
    Nothing,
    Map[String, Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]]]
  ] =
    val env = ZIO
      .foreach(paths) { p =>
        for {
          ds <- ZIO.serviceAt[DataSource](p)
          quill <- ZIO.succeed(Sqlite(NamingStrategy(SnakeCase, Escape), ds.get))
        } yield (p, quill.asInstanceOf[Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]]])
      }
      .map(t => (ZEnvironment(t.toMap)))
    ZLayer.fromZIOEnvironment(env)

end DataSourceLayer
