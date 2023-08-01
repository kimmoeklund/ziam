package fi.kimmoeklund.ziam

import com.outr.scalapass.Argon2PasswordFactory
import fi.kimmoeklund.html.Site
import fi.kimmoeklund.service.*
import io.getquill.{Escape, SnakeCase}
import zio.*
import zio.http.*
import zio.http.HttpAppMiddleware.{addCookie, basicAuthZIO, signCookies, whenRequestZIO, whenStatus}
import zio.logging.backend.SLF4J
import zio.metrics.*

import java.io.File

object Main extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j
  private val requestCounter = Metric.counter("requestCounter").fromConst(0)
  // todo: reactor to ZIO-config
  private val cookieSecret = "mysecret"
  private val domain = "localhost"
  private val authCookie = Cookie.Response("ziam", "", Some(domain), None, false, true)
  private val invalidCookie =
    (request: Request) => {
      val cookie =
        request.header(Header.Cookie).map(_.value.toChunk).getOrElse(Chunk.empty).find(_.name == authCookie.name)
      ZIO.succeed(if (cookie.isEmpty) then true else cookie.head.unSign(cookieSecret).isEmpty)
    }

  private val basicAuthAndAddCookie =
    basicAuthZIO(creds =>
      for {
        repo <- ZIO.serviceAt[UserRepository]("ziam")
        result <- repo.get
          .checkUserPassword(creds.uname, creds.upassword)
          .tapError(e => ZIO.logInfo(e.toString))
          .fold(_ => false, _ => true)
      } yield (result)
    ) >>> whenStatus(status => status != Status.Unauthorized)(
      addCookie(authCookie) >>> signCookies(cookieSecret)
    )

  private val scriptsAndMainPage = Http.collectHttp[Request] {
    case Method.GET -> Root / "scripts" =>
      Http.fromFile(File("../js/target/scala-3.3.0/ziam-fastopt/main.js"))
    case Method.GET -> Root => Handler.response(Response.redirect(URL(Root / "ziam" / "users"))).toHttp
  }

  val site = Site.build("ziam")

  val httpApps =
    (scriptsAndMainPage.withDefaultErrorResponse ++ site.httpValue) @@ whenRequestZIO(invalidCookie)(
      basicAuthAndAddCookie
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
