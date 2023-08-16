package fi.kimmoeklund.ziam

import com.outr.scalapass.Argon2PasswordFactory
import fi.kimmoeklund.html.{Site, SiteService}
import fi.kimmoeklund.service.*
import io.getquill.{Escape, SnakeCase}
import zio.*
import zio.http.*
import zio.http.HttpAppMiddleware.{
  addCookie,
  basicAuthZIO,
  signCookies,
  whenRequestZIO,
  whenStatus,
  customAuthZIO,
  redirect
}
import zio.logging.backend.SLF4J
import zio.metrics.*

import java.io.File
import java.time.Duration
import fi.kimmoeklund.html.pages.DefaultLoginPage

object Main extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j
  private val requestCounter = Metric.counter("requestCounter").fromConst(0)
  // todo: reactor to ZIO-config
  private val cookieSecret = "mysecret"
  private val authCookie = Cookie.Response("ziam", "", None, Some(Root), false, true).sign(cookieSecret)
  private val logoutCookie =
    Cookie.Response("ziam", "", None, Some(Root), false, true, Some(Duration.ZERO)).sign(cookieSecret)
  private val invalidCookie =
    (request: Request) => {
      val cookie =
        request.header(Header.Cookie).map(_.value.toChunk).getOrElse(Chunk.empty).find(_.name == authCookie.name)
      ZIO.succeed(if (cookie.isEmpty) then true else cookie.head.unSign(cookieSecret).isEmpty)
    }

  private val scriptsAndMainPage = Http.collectHttp[Request] {
    case Method.GET -> Root / db / page if (page.endsWith(".html") || page.endsWith(".css")) =>
      // TODO sanization and checking path
      Http.fromResource("html/" + page)
    case Method.GET -> Root / "scripts" =>
      Http.fromFile(File("../js/target/scala-3.3.0/ziam-fastopt/main.js"))
    case Method.GET -> Root => Handler.response(Response.redirect(URL(Root / "ziam" / "users"))).toHttp
  }


  val siteService = for {
    dbMgmt <- ZIO.service[DbManagement]
    sites <- dbMgmt.buildSites
  } yield (SiteService(sites, authCookie, logoutCookie))

  def run = {
    (siteService.provide(DbManagementLive.live).orDie).flatMap(siteService => {
      val databases = siteService.sites.map(_.db) 
      val httpApps =
      ZiamApi() ++ siteService.loginApp ++
        (scriptsAndMainPage.withDefaultErrorResponse ++ siteService.contentApp) 
//        @@ whenRequestZIO(invalidCookie)(
  //        redirect(URL(Root / "ziam" / "login"), false) // TODO fix the redirect (how ??), cookies should db specific
    //    ) 

      Server
      .serve(httpApps)
      .provide(
        Server.default,
        Argon2.passwordFactory,
        DataSourceLayer.quill(databases),
        DataSourceLayer.sqlite(databases),
        UserRepositoryLive.sqliteLayer(databases),
      )
    })
  }
