package fi.kimmoeklund.ziam

import fi.kimmoeklund.infra.UserRepositoryLive
import fi.kimmoeklund.html.permission.PermissionsPage
import zio.*
import zio.logging.{LogFormat, console}
import zio.http.*
import zio.logging.backend.SLF4J
import zio.metrics.*
import io.getquill.jdbczio.Quill
import io.getquill.SnakeCase
import io.getquill.Escape
import io.getquill.NamingStrategy
import java.io.File

object MainApp extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
  Runtime.removeDefaultLoggers >>> SLF4J.slf4j
  private val dataSourceLayer = Quill.DataSource.fromPrefix("ziam-db")
  private val postgresLayer =
    Quill.Postgres.fromNamingStrategy(NamingStrategy(SnakeCase, Escape))
  private val repoLayer = UserRepositoryLive.layer
  private val requestCounter = Metric.counter("requestCounter").fromConst(0)

  private val scripts = Http.collectHttp[Request] {
    case Method.GET -> Root / "scripts" =>
      Http.fromFile(new File("js/target/scala-3.3.0/ziam-fastopt/main.js"))
  }

  def run = {
    Server.serve(scripts.withDefaultErrorResponse ++ ZiamApi() ++ PermissionsPage()).provide(Server.default, dataSourceLayer, postgresLayer, repoLayer)
  }
