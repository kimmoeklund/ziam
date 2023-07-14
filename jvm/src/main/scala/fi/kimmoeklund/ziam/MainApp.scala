package fi.kimmoeklund.ziam

import fi.kimmoeklund.html.pages.{PermissionsPage, RolesPage, UsersPage}
import fi.kimmoeklund.infra.UserRepositoryLive
import fi.kimmoeklund.html.SiteMap
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

  private val scriptsAndMainPage = Http.collectHttp[Request] {
    case Method.GET -> Root / "scripts" =>
      Http.fromFile(new File("js/target/scala-3.3.0/ziam-fastopt/main.js"))
    //case Method.GET -> Root => Handler.fromFunction(_ => Response.html(SiteMap.usersPage.htmlValue(()))).toHttp
  }

  def run = {
    Server.serve(scriptsAndMainPage.withDefaultErrorResponse ++ PermissionsPage.httpValue ++ UsersPage.httpValue ++ RolesPage.httpValue
    ).provide(Server.default, dataSourceLayer, postgresLayer, repoLayer)
  }
