package fi.kimmoeklund.service

import io.getquill.jdbczio.Quill
import io.getquill.jdbczio.Quill.*
import io.getquill.{CompositeNamingStrategy2, Escape, NamingStrategy, SnakeCase}
import org.sqlite.SQLiteDataSource
import zio.*

import javax.sql.DataSource

object DataSourceLayer {

  def sqlite(key: String) = {
    val dataSource = for {
      config <- ZIO.config(DbConfig.config)
      foo <- ZIO.succeed({
        val ds = SQLiteDataSource()
        ds.setUrl(s"jdbc:sqlite:${config.dbLocation}/${key}.db")
        ds.asInstanceOf[DataSource]
      })
    } yield (
      ZEnvironment(
        Map(
          key -> foo
        )
      )
    )
    ZLayer.fromZIOEnvironment(dataSource)
  }

  def quill(
      path: String
  ): ZLayer[
    Map[String, DataSource],
    Nothing,
    Map[String, Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]]]
  ] = {
    val quill = for {
      ds <- ZIO.serviceAt[DataSource](path)
      quill <- ZIO.succeed(Sqlite(NamingStrategy(SnakeCase, Escape), ds.get))
    } yield (ZEnvironment(Map(path -> quill.asInstanceOf[Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]]])))
    ZLayer.fromZIOEnvironment(quill)
  }
}
