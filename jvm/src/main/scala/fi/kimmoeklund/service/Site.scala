package fi.kimmoeklund.service

import fi.kimmoeklund.html.pages.*
import fi.kimmoeklund.html.Page
import fi.kimmoeklund.html.htmlSnippet
import fi.kimmoeklund.html.NewResourceForm
import fi.kimmoeklund.service.{RoleRepository, UserRepository}
import zio.*
import zio.prelude.Newtype
import zio.http.html.*
import zio.http.html.Html.fromDomElement
import zio.http.{html as _, *}
import zio.http.RequestHandlerMiddlewares
import zio.http.HttpAppMiddleware.*
import zio.http.RequestHandlerMiddleware

case class Site(db: String, pages: List[Page[_, _, _]], loginPage: LoginPage[_], defaultPage: Page[_, _, _])

type Repositories = UserRepository & RoleRepository & PermissionRepository

object Site {
  def build(db: String, cookieSecret: CookieSecret) = {
    val pages = List(
      UsersPage("users", db),
      OrganizationsPage("organizations", db),
      RolesPage("roles", db),
      PermissionsPage("permissions", db)
    )
    Site(
      db,
      pages,
      DefaultLoginPage("login", "logout", db, cookieSecret),
      pages(0)
    )
  }
}

final case class SiteEndpoints(sites: Seq[Site]) {

  private def getPage(path: String, db: String) =
    ZIO
      .fromOption(for {
        site <- sites.find(s => s.db == db)
        page <- site.pages.find(f => f.path == path)
      } yield (site, page))
      .orElseFail(Response.status(Status.NotFound))

  private def siteWithLoginPage(db: String, path: String) = for {
    site <- sites.find(s => s.db == db)
    page <- if site.loginPage.loginPath == path then Some(site.loginPage) else None
  } yield (site)

  private def siteWithLogoutPage(db: String, path: String) = for {
    site <- sites.find(s => s.db == db)
    page <- if site.loginPage.logoutPath == path then Some(site.loginPage) else None
  } yield (site)

  val checkCookie = new RequestHandlerMiddleware.Simple[Any, Nothing] {
    override def apply[R1 <: Any, Err1 >: Nothing](
        handler: Handler[R1, Err1, Request, Response]
    )(implicit trace: Trace): Handler[R1, Err1, Request, Response] =
      Handler.fromFunctionZIO[Request] { request =>
        request.url.path.match {
          case Root / db / _ => {
            val site = sites.find(_.db == db)
            val decryptedCookie = for {
              cookie <- request.header(Header.Cookie).map(_.value.toChunk).getOrElse(Chunk.empty).find(_.name == db)
              decrypted <- cookie.unSign(CookieSecret.unwrap(site.get.loginPage.cookieSecret))
            } yield (decrypted)
            (site, decryptedCookie) match {
              case (Some(_), Some(_)) => handler(request)
              case (None, _)          => ZIO.succeed(Response.status(Status.NotFound))
              case (Some(site), None) => ZIO.succeed(Response.redirect(URL(Root / site.db / site.loginPage.loginPath)))
            }
          }
          case _ => ZIO.succeed(Response.status(Status.NotFound))
        }
      }
  }

  def loginApp: App[Map[String, Repositories]] = Http.collectZIO[Request] {
    case request @ Method.POST -> Root / db / path if siteWithLoginPage(db, path).isDefined => {
      val site = siteWithLoginPage(db, path).get
      site.loginPage
        .doLogin(request)
        .mapBoth(
          _ => Response.html(site.loginPage.showLogin),
          _ => Response.seeOther(URL(Root / db / "users.html#resource-users")).addCookie(site.loginPage.loginCookie)
        )
    }
    case request @ Method.GET -> Root / db / path if siteWithLoginPage(db, path).isDefined => {
      val site = siteWithLoginPage(db, path).get
      ZIO.succeed(Response.html(site.loginPage.showLogin))
    }

    case Method.GET -> Root / db / path if siteWithLogoutPage(db, path).isDefined => {
      val site = siteWithLogoutPage(db, path).get
      ZIO.succeed(Response.seeOther(URL(Root / site.db / site.defaultPage.path)).addCookie(site.loginPage.logoutCookie))
    }
  }

  def contentApp: App[Map[String, Repositories]] = Http.collectZIO[Request] {
    case request @ Method.GET -> Root / db / path / format =>
      getPage(path, db).flatMap((site, page) =>
        format match {
          case "options" =>
            val selected = request.url.queryParams.get("selected") // .map(_.asString.split(",").toSeq)
            ZIO.logInfo(s"options selected query param: $selected") *>
              page
                .optionsList(selected)
                .mapBoth(
                  e => Response.text(e.toString).withStatus(Status.InternalServerError),
                  (p: Html) => htmlSnippet(p)
                )
          case "th" => ZIO.succeed(htmlSnippet(page.tableHeaders.fold(Html.fromUnit(()))(_ ++ _)))
          case "form" if page.isInstanceOf[NewResourceForm[_]] =>
            ZIO.succeed(htmlSnippet(page.asInstanceOf[NewResourceForm[_]].htmlForm()))
          case _ => ZIO.succeed(Response.status(Status.UnsupportedMediaType))
        }
      )

    case Method.GET -> Root / db / path =>
      getPage(path, db).flatMap((site, page) =>
        page.tableRows.foldZIO(
          e => {
            for {
              response <- ZIO.succeed(Response.text(e.toString).withStatus(Status.InternalServerError))
            } yield response
          },
          (result: Seq[Dom]) => ZIO.succeed(htmlSnippet(result))
        )
      )
    case req @ Method.POST -> Root / db / path =>
      getPage(path, db).flatMap((site, page) =>
        page
          .post(req)
          .foldCauseZIO(
            { case c: Cause[_] =>
              ZIO.succeed(
                Response
                  .text(
                    s"${c.failures.map(e => e.toString).mkString("\n")}\n${c.defects.map(d => d.getMessage).mkString("\n")}"
                  )
                  .withStatus(Status.BadRequest)
              )
            },
            p =>
              ZIO.succeed(
                htmlSnippet(p).addHeader("HX-Trigger-After-Swap", "resetAndFocusForm")
              )
          )
      )

    case Method.DELETE -> Root / db / path / id =>
      getPage(path, db).flatMap((site, page) =>
        page
          .delete(id)
          .foldZIO(_ => ZIO.succeed(Response.status(Status.BadRequest)), _ => ZIO.succeed(Response.status(Status.Ok)))
      )
  }
}
