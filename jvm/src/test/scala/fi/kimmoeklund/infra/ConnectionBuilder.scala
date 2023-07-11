package fi.kimmoeklund.infra

import javax.sql.DataSource
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.postgresql.ds.PGSimpleDataSource
import zio._

trait DataSourceBuilder:
  def dataSource: DataSource

final class DataSourceBuilderLive(container: PostgreSQLContainer)
    extends DataSourceBuilder:
  val dataSource: DataSource =
    val ds = new PGSimpleDataSource()
    ds.setUrl(container.jdbcUrl)
    ds.setUser(container.username)
    ds.setPassword(container.password)
    ds

object DataSourceBuilderLive:
  val layer: ZLayer[PostgreSQLContainer, Nothing, DataSourceBuilder] =
    ZLayer(
      ZIO
        .service[PostgreSQLContainer]
        .map(container => DataSourceBuilderLive(container))
    )
