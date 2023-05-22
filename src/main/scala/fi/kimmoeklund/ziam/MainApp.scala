package fi.kimmoeklund.ziam

import fi.kimmoeklund.infra.UserRepositoryLive
import zio.*
import zio.logging.{LogFormat, console}
import zio.http.Server
import zio.logging.backend.SLF4J
import zio.metrics.*
import io.getquill.jdbczio.Quill
import io.getquill.SnakeCase
import io.getquill.Escape
import io.getquill.NamingStrategy
object MainApp extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
//    Runtime.removeDefaultLoggers >>> console(LogFormat.default)
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j
//  val containerLayer = ZLayer.scoped(PostgresContainer.make())
  // val dataSourceLayer = ZLayer(ZIO.service[DataSourceBuilder].map(_.dataSource))
  // ZLayer jossa on datasource (hikari tms)
  val dataSourceLayer = Quill.DataSource.fromPrefix("ziam-db")
  val postgresLayer =
    Quill.Postgres.fromNamingStrategy(NamingStrategy(SnakeCase, Escape))
  val repoLayer = UserRepositoryLive.layer

  val requestCounter = Metric.counter("requestCounter").fromConst(0)
  def run = {
    Server.serve(ZiamApi()).provide(Server.default, dataSourceLayer, postgresLayer, repoLayer)
  }
