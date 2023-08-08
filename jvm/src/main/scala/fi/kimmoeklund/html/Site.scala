package fi.kimmoeklund.html

import fi.kimmoeklund.html.pages.*
import fi.kimmoeklund.service.{ UserRepository, PageService }
import zio.*
import zio.http.html.*
import zio.http.html.Html.fromDomElement
import zio.http.{html as _, *}

case class Site[R](
    db: String,
    tabMenu: TabMenu,
    pages: List[Page[R, _, _]],
    loginPage: LoginPage[R, _],
    defaultPage: Page[R, _, _]
)

final case class SiteService[R](
    sites: Seq[Site[R]],
    authCookie: Cookie.Response,
    logoutCookie: Cookie.Response
) {

  private def getPage(path: String, db: String) =
    ZIO
      .fromOption(for {
        site <- sites.find(s => s.db == db)
        page <- site.pages.find(f => f.path == path)
      } yield (site, page))
      .orElseFail(Response.status(Status.NotFound))

  private def siteWithLoginPage(db: String, path: String) = for {
    site <- sites.find(s => s.db == db)
    page <- if site.loginPage.loginPath == path then Some(site.loginPage) else None //
  } yield (site)

  private def siteWithLogoutPage(db: String, path: String) = for {
    site <- sites.find(s => s.db == db)
    page <- if site.loginPage.logoutPath == path then Some(site.loginPage) else None //
  } yield (site)

  private def setActive(site: Site[R], page: Page[R, _, _]) = site.tabMenu.setActiveTab(Root / site.db / page.path)

  def htmlValue(site: Site[R], page: Html): Html = html(
    htmxHead ++ body(div(classAttr := "container" :: Nil, site.tabMenu.htmlValue, a(hrefAttr := site.loginPage.logoutPath, "Logout"), div(classAttr := "container" :: Nil, page)))
  )

  def loginApp: App[Map[String, R]] = Http.collectZIO[Request] {
    case request @ Method.POST -> Root / db / path if siteWithLoginPage(db, path).isDefined => {
      val site = siteWithLoginPage(db, path).get
      site.loginPage
        .doLogin(request)
        .mapBoth(
          _ => Response.html(site.loginPage.showLogin),
          _ => Response.seeOther(URL(Root / db / "users")).addCookie(authCookie)
        )
    }
    case request @ Method.GET -> Root / db / path if siteWithLoginPage(db, path).isDefined => {
      val site = siteWithLoginPage(db, path).get
      ZIO.succeed(Response.html(site.loginPage.showLogin))
    }

    case Request @ Method.GET -> Root / db / path if siteWithLogoutPage(db, path).isDefined => {
      val site = siteWithLogoutPage(db, path).get
      ZIO.succeed(Response.seeOther(URL(Root / site.db / site.defaultPage.path )).addCookie(logoutCookie))
    }


  }

  def contentApp: App[Map[String, R]] = Http.collectZIO[Request] {
    case request @ Method.GET -> Root / db / path / format =>
      format match {
        case "options" =>
          getPage(path, db).flatMap((site, page) =>
            page.optionsList
              .mapBoth(
                e => Response.text(e.toString).withStatus(Status.InternalServerError),
                (p: Html) => htmlSnippet(p)
              )
          )

        case _ => ZIO.succeed(Response.status(Status.UnsupportedMediaType))
      }

    case Method.GET -> Root / db / path =>
      getPage(path, db).flatMap((site, page) =>
        setActive(site, page)
        page.tableList.foldZIO(
          e => {
            for {
              _ <- ZIO.logInfo(e.toString)
              response <- ZIO.succeed(Response.text(e.toString).withStatus(Status.InternalServerError))
            } yield response
          },
          (result: Html) => ZIO.succeed(Response.html(htmlValue(site, result)))
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

object Site {
  def build(db: String) = {
    println("build site: " + db)
    val pages = List(
      UsersPage("users", db),
      OrganizationsPage("organizations", db),
      RolesPage("roles", db),
      PermissionsPage("permissions", db)
    )
    val tabs = pages.map(p => Tab(p.path.capitalize, Root / db / p.path, false))
    Site(db, TabMenu(tabs), pages, DefaultLoginPage("login", "logout", db), pages(0))
  }
}
