package fi.kimmoeklund.html

import fi.kimmoeklund.domain.{Organization, Permission, Role, User}
import fi.kimmoeklund.html.pages.*
import fi.kimmoeklund.service.UserRepository
import zio.*
import zio.http.html.*
import zio.http.html.Html.fromDomElement
import zio.http.{html as _, *}

case class Site(
    db: String,
    tabMenu: TabMenu,
    pages: List[SimplePage[_, _]]
) {

  private def getPage(path: String) =
    ZIO.fromOption(pages.find(_.path == path)).orElseFail(Response.status(Status.NotFound))

  private def setActive(page: SimplePage[_, _]) = tabMenu.setActiveTab(Root / db / page.path)

  def htmlValue(page: SimplePage[_, _], result: List[T]): Html =
    html(htmxHead ++ body(div(classAttr := "container" :: Nil, tabMenu.htmlValue, page.htmlValue(result))))

  def httpValue = Http.collectZIO[Request] {
    case Method.GET -> Root / this.db / path / format =>
      getPage(path).flatMap(page =>
        page.functions.getEffect
          .foldZIO(
            e => ZIO.succeed(Response.text(e.toString).withStatus(Status.InternalServerError)),
            (result: List[T]) =>
              format match
                case "options" => ZIO.succeed(htmlSnippet(page.functions.optionsList(result)))
                case _         => ZIO.succeed(Response.html(htmlValue(page, result)))
          )
      )

    case Method.GET -> Root / this.db / path =>
      getPage(path).flatMap(page =>
        page.functions.getEffect.foldZIO(
          e => {
            for {
              _ <- ZIO.logInfo(e.toString)
              response <- ZIO.succeed(Response.text(e.toString).withStatus(Status.InternalServerError))
            } yield response
          },
          (result: List[T]) => ZIO.succeed(Response.html(htmlValue(page, result)))
        )
      )

    case req @ Method.POST -> Root / this.db / path =>
      getPage(path).flatMap(page =>
        page.functions
          .postEffect(req)
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
                htmlSnippet(page.functions.postResult(p)).addHeader("HX-Trigger-After-Swap", "resetAndFocusForm")
              )
          )
      )

    case Method.DELETE -> Root / this.db / path / id =>
      getPage(path).flatMap(page =>
        page.functions
          .deleteEffect(id)
          .foldZIO(_ => ZIO.succeed(Response.status(Status.BadRequest)), _ => ZIO.succeed(Response.status(Status.Ok)))
      )
  }
}

object Site {
  def build(db: String) = {
    val pages = List(
      SimplePage[UserRepository, User]("users", UsersEffects),
      SimplePage[UserRepository, Organization]("organizations", OrganizationsEffects),
      SimplePage[UserRepository, Permission]("permissions", PermissionEffects),
      SimplePage[UserRepository, Role]("roles", RolesEffects)
    )
    val tabs = pages.map(p => Tab(p.path.capitalize, Root / db / p.path, false))
    Site(db, TabMenu(tabs), pages)
  }
}

//object SiteMap {
//  val organizationsTab = Tab("Organizations", Root / "organizations")
//  val usersTab = Tab("Users", Root / "users")
//  val permissionsTab = Tab("Permissions", Root / "permissions")
//  val rolesTab = Tab("Roles", Root / "roles")
//  val tabs = TabMenu(
//    List(
//      organizationsTab,
//      usersTab,
//      permissionsTab,
//      rolesTab
//    )
//  )
//}
