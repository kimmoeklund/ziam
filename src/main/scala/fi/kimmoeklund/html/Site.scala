package fi.kimmoeklund.html

import fi.kimmoeklund.domain.*
import fi.kimmoeklund.html.*
import fi.kimmoeklund.html.encoder.*
import fi.kimmoeklund.html.forms.*
import fi.kimmoeklund.html.pages.*
import fi.kimmoeklund.repository.*
import fi.kimmoeklund.service.Secrets
import zio.*
import zio.http.*
import zio.http.Method.*
import zio.http.Path.*
import zio.http.codec.PathCodec.literal
import zio.http.codec.PathCodec
import zio.http.template.{Dom, Html, div, idAttr}

import scala.reflect.TypeTest
import fi.kimmoeklund.templates.html.crud_page
import fi.kimmoeklund.templates.html.site

case class Site(
    db: String,
    loginPage: LoginPage[_],
    routes: Routes[Map[String, QuillCtx] & UserRepositoryLive & PermissionRepository & RoleRepository, Response]
):
  def checkCookie: Middleware[Any] =
  new Middleware[Any] {
    override def apply[Env1 <: Any, Err](routes: Routes[Env1, Err]): Routes[Env1, Err] =
      routes.transform { handler =>
          Handler.fromFunctionZIO { request =>
            println("foo")
            ZIO.log(s"request.path.segments: ${request.path.segments}") *>
              request.path.segments.toList.match {
                case db :: site :: "page" :: tail =>
                  println("foo2")
                  val decrypted = for 
                    cookie <- request.cookie(site)
                    decrypted <- cookie.toRequest.unSign(loginPage.cookieSecret.toString)
                  yield (decrypted)
                  if (decrypted.isDefined) then handler(request) else ZIO.succeed(Response.redirect(URL((Path.root ++ loginPage.loginPath))))
                case _ => 
                  println("foo3")
                  handler(request) //TODO allow only login and others ZIO.succeed(Response.status(Status.NotFound))
              }
        }
      }
  }
  def routesWithCookieCheck = this.routes @@ checkCookie
end Site  

case class CrudPageParams(pageName: String, description: String, addButtonLabel: String, resourcePath: String)

object Site {


  def response(content: String) =
      Response(
        Status.Ok,
        Headers(Header.ContentType(MediaType.text.html)),
        Body.fromCharSequence(content)
      )
  def pageParams[R](page: Page[R], db: String) = CrudPageParams(page.name, s"List of ${page.name}", s"Add ${page.name}", page.path.encode)

  private def buildPageRoutes[R](using QuillCtx)(pages: Seq[CrudPage[_, _, _, _]], page: CrudPage[R, _, _, _], db: String) =
    Chunk(
      Method.GET / literal("page") / page.path.segments.last -> handler:
        (request: Request) =>
          val id = request.url.queryParam("id")
          for
            table <-page.renderAsTable             
            form <- page.getAsForm(id)
          yield(response(site(pages, page.name, crud_page(pageParams(page, db), table, form, id.isDefined)).body)),        
      Method.GET / literal("page") / page.path.segments.last / "new" -> handler:
          for
            table <-page.renderAsTable             
            form <- page.getAsForm(None)
          yield(response(site(pages, page.name, crud_page(pageParams(page, db), table, form, true)).body)),        
      Method.POST / literal("page") / page.path.segments.last -> handler: 
        (request: Request) => page.upsert(request).map(htmlSnippet(_)),
      Method.DELETE / literal("page") / page.path.segments.last / string("id") -> handler:
       (id: String, request: Request) => page.delete(id).map(htmlSnippet(_)),
      Method.GET / literal("page") / page.path.segments.last / string("format") -> handler:
        (format: String, request: Request) => format match 
          case "options" =>
            page.renderAsOptions(request.url.queryParams("selected")).mapBoth((e: ErrorMsg) => Response.internalServerError(e.msg), htmlSnippet(_))
          case _ => ZIO.succeed(Response.status(Status.UnsupportedMediaType))
    )

  def layer = for {
    cookieSecret <- ZIO.config(Secrets.cookieSecret)
    db           <- ZIO.service[Map[String, QuillCtx]]
    sites <- ZIO.succeed(db.toList.map({ case (dbName, quillCtx) =>
      given QuillCtx = quillCtx
      Site.build(dbName, cookieSecret)
    }))
  } yield (sites)
    
  def build(using QuillCtx)(db: String, cookieSecret: CookieSecret) = {
    val basePath = Path("site") / db / "page"
    val usersPage       = UsersPage(basePath / "users", db, "Users").asInstanceOf[CrudPage[UserRepositoryLive & PermissionRepository, _, _ ,_]]
    val permissionsPage = PermissionsPage(basePath / "permissions", db, "Permissions").asInstanceOf[CrudPage[PermissionRepository, _, _, _]]
    val rolesPage       = RolesPage(basePath / "roles", db, "Roles").asInstanceOf[CrudPage[RoleRepository & PermissionRepository, _, _, _]]

    val pages = Seq(usersPage, rolesPage, permissionsPage)
    val pageRoutes = pages.flatMap(buildPageRoutes(pages, _, db))
    val loginPage = DefaultLoginPage(Path("site") / db / "login", Path("site") / db / "logout", db, cookieSecret);
    // login routes omitted atm, and there's no cookie check
    val loginRoutes =
      Chunk(
        Method.POST / loginPage.loginPath.segments.last -> handler { (request: Request) =>
          println("post poling")
          loginPage
            .doLogin(request)
            .tapError(e => ZIO.log(s"login failed: ${e}"))
            .mapBoth(
              _ => response(loginPage.showLogin),
              _ => Response.seeOther(URL(Path.root ++ usersPage.path)).addCookie(loginPage.loginCookie)
            )
        },
        Method.GET / loginPage.loginPath.segments.last -> handler {
          println("show login")
          response(loginPage.showLogin)
        },
        Method.GET / loginPage.logoutPath.segments.last -> handler {
          Response.seeOther(URL(Path.root ++ loginPage.loginPath)).addCookie(loginPage.logoutCookie)
        }
      )
    Site(
      db,
      loginPage,
      literal("site") / db / Routes.fromIterable(pageRoutes ++ loginRoutes) 
    )
  }
}
