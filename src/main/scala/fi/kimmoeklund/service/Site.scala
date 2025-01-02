package fi.kimmoeklund.service

import fi.kimmoeklund.html.pages.*
import fi.kimmoeklund.html.forms.*
import fi.kimmoeklund.domain.*
import fi.kimmoeklund.html.{CrudPage, Page, htmlSnippet}
import zio.*
import zio.http.HttpAppMiddleware.*
import zio.http.html.*
import zio.http.{RequestHandlerMiddleware, html as _, *}
import fi.kimmoeklund.html.HtmlEncoder
import fi.kimmoeklund.html.ElementTemplate
import fi.kimmoeklund.html.ErrorMsg
import fi.kimmoeklund.html.emptyHtml

type Repositories = UserRepository & RoleRepository & PermissionRepository

case class Site[-R <: Repositories](
    db: String,
    pages: List[Page[R, _, _]],
    loginPage: LoginPage[_],
    defaultPage: Page[R, _, _]
)

type RepositoriesIntersect = UserRepository & RoleRepository & PermissionRepository
type Resources             = User | Role | Permission
type Views                 = UserView & RoleView & Permission
type Pages                 = UsersPage & RolesPage & PermissionsPage
type Forms                 = UserForm | RoleForm | PermissionForm

given HtmlEncoder[Forms] with {
  def encodeParams(
      template: ElementTemplate,
      paramName: String = "",
      annotations: Seq[Any] = Seq.empty,
      value: Option[Forms] = None
  ): List[Html] = value match {
    case Some(form: UserForm) => HtmlEncoder[UserForm].encodeParams(template, paramName, annotations, Some(form))
    case Some(form: RoleForm) => HtmlEncoder[RoleForm].encodeParams(template, paramName, annotations, Some(form))
    case Some(form: PermissionForm) =>
      HtmlEncoder[PermissionForm].encodeParams(template, paramName, annotations, Some(form))
    case None => List.empty
  }
  def encodeValues(
      template: ElementTemplate,
      value: Forms,
      errors: Option[Seq[ErrorMsg]] = None,
      paramName: Option[String] = None,
      annotations: Seq[Any] = Seq.empty
  ): List[Html] = value match {
    case form: UserForm => HtmlEncoder[UserForm].encodeValues(template, form, errors, paramName, annotations)
    case form: RoleForm => HtmlEncoder[RoleForm].encodeValues(template, form, errors, paramName, annotations)
    case form: PermissionForm =>
      HtmlEncoder[PermissionForm].encodeValues(template, form, errors, paramName, annotations)
  }
}

object Site {
  def build[R <: Repositories](db: String, cookieSecret: CookieSecret) = {
    val pages = List(
      UsersPage("users", db),
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

final case class SiteEndpoints[R <: Repositories](sites: Seq[Site[R]]) {

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
              cookie    <- request.header(Header.Cookie).map(_.value.toChunk).getOrElse(Chunk.empty).find(_.name == db)
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

  def loginApp = Http.collectZIO[Request] {
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

  def contentApp = Http.collectZIO[Request] {
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
          case "form" =>
            page match {
              case crudPage: CrudPage[R, CrudResource[Resources, Forms], _, Forms] =>
                val id = request.url.queryParams.get("id").map(_.asString)
                println("id: " + id)
                val effect = if (id.isDefined && id.get != "null") {
                  crudPage
                    .get(id.get)
                    .map(r => r.form)
                    .map(form => Html.fromDomElement(div(idAttr := "form-response", crudPage.htmlForm(Some(form)))))
                } else {
                  println("empty form")
                  ZIO.succeed(
                    Html.fromDomElement(div(idAttr := "form-response", crudPage.htmlForm(Some(crudPage.emptyForm))))
                  )
                }
                effect.mapBoth(
                  e => Response.text(e.toString).withStatus(Status.InternalServerError),
                  (p: Html) => htmlSnippet(Html.fromDomElement(div(idAttr := "form-response", p)))
                )
              case _ => ZIO.logInfo("iam here") *> ZIO.fail(Response.status(Status.UnsupportedMediaType))
            }
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

    case req @ (Method.POST) -> Root / db / path =>
      getPage(path, db).flatMap((site, page) =>
        page
          .upsertResource(req)
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
              ZIO.succeed({
                val html = htmlSnippet(p)
                if req.method == Method.POST then html.addHeader("HX-Trigger-After-Swap", "resetAndFocusForm")
                html
              })
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
