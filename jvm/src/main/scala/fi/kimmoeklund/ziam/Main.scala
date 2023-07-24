package fi.kimmoeklund.ziam

import com.outr.scalapass.Argon2PasswordFactory
import fi.kimmoeklund.domain.User
import fi.kimmoeklund.html.SiteMap
import fi.kimmoeklund.html.pages.*
import fi.kimmoeklund.infra.UserRepositoryLive
import io.getquill.jdbczio.Quill
import io.getquill.{Escape, NamingStrategy, SnakeCase}
import zio.*
import zio.http.*
import zio.logging.backend.SLF4J
import zio.logging.{LogFormat, console}
import zio.metrics.*

import java.io.File
import scala.util.Try

object Main extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j
  private val dataSourceLayer = Quill.DataSource.fromPrefix("ziam-db")
  private val postgresLayer =
    Quill.Postgres.fromNamingStrategy(NamingStrategy(SnakeCase, Escape))
  private val passwordFactory: ZLayer[Any, Throwable, Argon2PasswordFactory] = ZLayer.scoped {
    for {
      factory <- ZIO.attempt(Argon2PasswordFactory(parallelism = 1, memory = 100 * 1024))
    } yield (factory)
  }
  private val repoLayer = UserRepositoryLive.layer
  private val requestCounter = Metric.counter("requestCounter").fromConst(0)

  private val scriptsAndMainPage = Http.collectHttp[Request] {
    case Method.GET -> Root / "scripts" => {
      Http.fromFile(File("../js/target/scala-3.3.0/ziam-fastopt/main.js"))
    }
    case Method.GET -> Root => Handler.response(Response.redirect(URL(UsersPage.path))).toHttp
  }

  def run = {
    Server
      .serve(
        scriptsAndMainPage.withDefaultErrorResponse
          ++ PermissionsPage.httpValue
          ++ UsersPage.httpValue
          ++ RolesPage.httpValue
          ++ OrganizationsPage.httpValue ++ ZiamApi()
      )
      .provide(Server.default, dataSourceLayer, postgresLayer, passwordFactory, repoLayer)
  }
