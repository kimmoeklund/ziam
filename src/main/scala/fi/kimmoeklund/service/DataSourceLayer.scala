package fi.kimmoeklund.service

import io.getquill.jdbczio.Quill
import io.getquill.jdbczio.Quill.*
import io.getquill.{CompositeNamingStrategy2, Escape, NamingStrategy, SnakeCase}
import org.sqlite.{SQLiteConfig, SQLiteDataSource}
import zio.*

import javax.sql.DataSource

object DataSourceLayer:

  private val config =
    val c = SQLiteConfig()
    c.enforceForeignKeys(true)
    c

  def sqlite = for {
    keys <- ZIO.service[Seq[String]]
    dataSources <- ZIO.foreach(keys) { key =>
      for {
        config <- ZIO.config(DbConfig.config)
        ds <- ZIO.succeed({
          val ds = SQLiteDataSource()
          ds.setUrl(s"jdbc:sqlite:${config.dbLocation}/${key}.db")
          ds.setConfig(this.config)
          ds.asInstanceOf[DataSource]
        })
      } yield (key, ds)
    }
  } yield (Map.from(dataSources))

  def quill = for {
    dataSources <- ZIO.service[Map[String, DataSource]]
    quills <- ZIO.foreach(dataSources) { case (k, v) =>
      ZIO.succeed((k, Sqlite(NamingStrategy(SnakeCase, Escape), v)
        .asInstanceOf[Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]]]))
    }
  } yield (quills)

end DataSourceLayer
