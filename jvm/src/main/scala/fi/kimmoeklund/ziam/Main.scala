package fi.kimmoeklund.ziam

import com.outr.scalapass.Argon2PasswordFactory
import fi.kimmoeklund.html.Site
import fi.kimmoeklund.service.*
import io.getquill.{Escape, SnakeCase}
import zio.*
import zio.http.*
import zio.http.HttpAppMiddleware.{addCookie, basicAuthZIO, signCookies, whenRequestZIO, whenStatus, customAuthZIO, redirect}
import zio.logging.backend.SLF4J
import zio.metrics.*

import java.io.File
import java.time.Duration
import fi.kimmoeklund.html.pages.LoginPage

object Main extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j
  private val requestCounter = Metric.counter("requestCounter").fromConst(0)
  // todo: reactor to ZIO-config
  private val cookieSecret = "mysecret"
  private val authCookie = Cookie.Response("ziam", "", None, Some(Root), false, true).sign(cookieSecret)
  private val logoutCookie = Cookie.Response("ziam", "", None, Some(Root), false, true, Some(Duration.ZERO)).sign(cookieSecret)
  private val invalidCookie =
    (request: Request) => {
      val cookie =
        request.header(Header.Cookie).map(_.value.toChunk).getOrElse(Chunk.empty).find(_.name == authCookie.name)
      ZIO.succeed(if (cookie.isEmpty) then true else cookie.head.unSign(cookieSecret).isEmpty)
    }

  private val scriptsAndMainPage = Http.collectHttp[Request] {
    case Method.GET -> Root / "scripts" =>
      Http.fromFile(File("../js/target/scala-3.3.0/ziam-fastopt/main.js"))
    case Method.GET -> Root => Handler.response(Response.redirect(URL(Root / "ziam" / "users"))).toHttp
  }

  val site = Site.build("ziam")
  val loginPage = LoginPage("login", "logout", "ziam", authCookie, logoutCookie)
  val httpApps = loginPage.loginApp ++ 
    (scriptsAndMainPage.withDefaultErrorResponse ++ site.httpValue) @@ whenRequestZIO(invalidCookie)(
      redirect(URL(Root / "ziam" / "login"), false)
    ) ++ ZiamApi() 

  def run = {
    Server
      .serve(httpApps)
      .provide(
        Server.default,
        Argon2.passwordFactory,
        DataSourceLayer.quill("ziam"),
        DataSourceLayer.sqlite("ziam"),
        UserRepositoryLive.sqliteLayer("ziam")
      )
  }
