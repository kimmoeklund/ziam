package fi.kimmoeklund.ziam

import com.outr.scalapass.Argon2PasswordFactory
import fi.kimmoeklund.html.Site
import fi.kimmoeklund.repository.*
import fi.kimmoeklund.service.*
import zio.*
import zio.logging.backend.SLF4J

import java.io.File
import javax.sql.DataSource
import zio.http.Server
import zio.http.Middleware
import zio.http.Path

object Main extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  val dbNameLayer = ZLayer.fromZIO(ZIO.serviceWithZIO[DbManagement](_.getDatabases))
  val dsLayer     = ZLayer.fromZIO(DataSourceLayer.sqlite)
  val quills      = ZLayer.fromZIO(DataSourceLayer.quill)
  val sites       = ZLayer.fromZIO(Site.layer)
  val routes = ZIO
    .service[List[Site]]
    .map(_.map(_.routes).reduce(_ ++ _))
  val dataLayers = ZLayer.fromZIO(ZIO.service[Argon2PasswordFactory].map(UserRepositoryLive(_)))
    ++ ZLayer.succeed[RoleRepository](RoleRepositoryLive())
    ++ ZLayer.succeed[PermissionRepository](PermissionRepositoryLive())

  def cleanup = {
    ZIO.unit
  }

  def run = routes
    .provide(DbManagementLive.live, dbNameLayer, dsLayer, quills, sites)
    .orDie
    .flatMap(r =>
        Server
          .serve(r @@ Middleware.serveResources(Path("assets"), "static"))
          .provide(
            Server.default,
            quills,
            dataLayers,
            Argon2.passwordFactory,
            dsLayer,
            dbNameLayer,
            DbManagementLive.live
          )
    )
