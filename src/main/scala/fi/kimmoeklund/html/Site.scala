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
import fi.kimmoeklund.templates.html.chat
import scala.util.Try
import java.util.UUID

case class RequestContext(requestId: String, user: User) 

case class Site(
    db: String,
    loginPage: LoginPage[?],
    loginRoutes: Routes[Map[String, QuillCtx] & UserRepositoryLive, Response],
    pageRoutes: Routes[Map[String, QuillCtx] & UserRepositoryLive & PermissionRepository & RoleRepository, Response]
)

case class CrudPageParams(pageName: String, description: String, addButtonLabel: String, resourcePath: String)

object Site {


  def response(content: String) =
      Response(
        Status.Ok,
        Headers(Header.ContentType(MediaType.text.html)),
        Body.fromCharSequence(content)
      )
  def pageParams[R](page: Page[R], db: String) = CrudPageParams(page.name, s"List of ${page.name}", s"Add ${page.name}", page.path.encode)

  private def buildPageRoutes[R](using QuillCtx)(pages: Seq[CrudPage[?, ?, ?, ?]], page: CrudPage[R, ?, ?, ?], db: String) =
    val pagePathCodec = PathCodec.literal(page.path.encode)
    Chunk(
      Method.GET / pagePathCodec -> handler:
        (request: Request) => 
          val id = request.url.queryParam("id")
          withContext((requestCtx: RequestContext) =>
            for
              table <-page.renderAsTable             
              form <- page.getAsForm(id)
            yield(response(site(pages, page.name, requestCtx.user, crud_page(pageParams(page, db), table, form, id.isDefined)).body))
          ),        
      Method.GET / pagePathCodec / "new" -> handler:
          withContext((requestCtx: RequestContext) =>
            for
              table <-page.renderAsTable             
              form <- page.getAsForm(None)
            yield(response(site(pages, page.name, requestCtx.user, crud_page(pageParams(page, db), table, form, true)).body))
          ),        
      Method.POST / pagePathCodec -> handler: 
        (request: Request) => page.upsert(request).map(htmlSnippet(_)),
      Method.DELETE / pagePathCodec / string("id") -> handler:
       (id: String, request: Request) => page.delete(id).map(htmlSnippet(_)),
      Method.GET / pagePathCodec / string("format") -> handler:
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
    val basePath = Path("site") / db 
    val pageBasePath = basePath / "page" 
    val usersPage       = UsersPage(pageBasePath / "users", db, "Users").asInstanceOf[CrudPage[UserRepositoryLive & PermissionRepository, ?, ? ,?]]
    val permissionsPage = PermissionsPage(pageBasePath / "permissions", db, "Permissions").asInstanceOf[CrudPage[PermissionRepository, ?, ?, ?]]
    val rolesPage       = RolesPage(pageBasePath / "roles", db, "Roles").asInstanceOf[CrudPage[RoleRepository & PermissionRepository, ?, ?, ?]]
    val chatPage        = ChatPage(pageBasePath / "chat", "Chat")

    val pages = Seq(usersPage, rolesPage, permissionsPage)
    val pageRoutes = pages.flatMap(buildPageRoutes(pages, _, db))
    val loginPage = DefaultLoginPage(basePath / "login", basePath / "logout", db, cookieSecret);
    val loginPathCodec = PathCodec.literal(loginPage.loginPath.encode)
    val logoutPathCodec = PathCodec.literal(loginPage.logoutPath.encode)
    val webSocketApp = Handler.webSocket { channel => 
      channel.receiveAll {
        case ChannelEvent.Read(WebSocketFrame.Text(_)) => 
          channel.send(ChannelEvent.Read(WebSocketFrame.text("bar")))
        case _ => 
          ZIO.unit
      }
    }
    val websocketRoute = Chunk(Method.GET / literal("site") / db / literal("chat") / literal("websocket") -> handler(webSocketApp.toResponse))
    val chatRoute = Chunk(Method.GET / literal("site") / db / literal("chat") -> handler:
        response(site(pages, "Chat", User(UserId.create, "dummy user", Set(), Set()), chat()).body))

    val loginRoutes = 
      Chunk(
        Method.POST / loginPathCodec -> handler { (request: Request) =>
          loginPage
            .doLogin(request)
            .tapError(e => ZIO.log(s"login failed: ${e}"))
            .mapBoth(
              _ => response(loginPage.showLogin),
              user => Response.seeOther(URL(Path.root ++ usersPage.path)).addCookie(loginPage.loginCookie(user))
           )
        },
        Method.GET / loginPathCodec -> handler {
          response(loginPage.showLogin)
        },
        Method.GET / logoutPathCodec -> handler {
          Response.seeOther(URL(Path.root ++ loginPage.loginPath)).addCookie(loginPage.logoutCookie)
        }
      )
    Site(
      db,
      loginPage,
      Routes.fromIterable(loginRoutes ++ websocketRoute ++ chatRoute),
      Routes.fromIterable(pageRoutes) @@ CookieAspectHandler.checkCookie(loginPage.loginPath, loginPage.cookieSecret),
    )
  }
}
