package fi.kimmoeklund.ziam

import com.outr.scalapass.Argon2PasswordFactory
import fi.kimmoeklund.html.pages.DefaultLoginPage
import fi.kimmoeklund.service.{Site, SiteEndpoints}
import fi.kimmoeklund.service.*
import io.getquill.{Escape, SnakeCase}
import zio.*
import zio.http.*
import zio.http.HttpAppMiddleware.*
import zio.logging.backend.SLF4J
import zio.metrics.*

import java.io.File
import io.getquill.jdbczio.Quill
import io.getquill.CompositeNamingStrategy2
import io.getquill.jdbczio.Quill.Sqlite

object Main extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val staticAssets = Http.collectHttp[Request] {
    case Method.GET -> Root / db / page if (page.endsWith(".html") || page.endsWith(".css")) =>
      Http.fromResource("html/" + page)
  }

  val siteService = for {
    dbMgmt <- ZIO.service[DbManagement]
    sites <- dbMgmt.buildSites
  } yield (SiteEndpoints(sites))

  def run = {
    (siteService
      .provide(DbManagementLive.live)
      .orDie)
      .flatMap(siteService => {
        val databases = siteService.sites.map(_.db)
        val httpApps =
          ZiamApi.app ++ siteService.loginApp ++ staticAssets.withDefaultErrorResponse ++ siteService.contentApp @@ siteService.checkCookie
        Server
          .serve(httpApps)
          .provide(
            Server.default,
            Argon2.passwordFactory,
            DataSourceLayer.quill(databases),
            DataSourceLayer.sqlite(databases),
            UserRepositoryLive.sqliteLayer(databases) ++ RoleRepositoryLive
              .sqliteLayer(databases) ++ PermissionRepositoryLive
              .layer(databases)
          )
      })
  }
