package fi.kimmoeklund.html

import fi.kimmoeklund.html.pages.*
import fi.kimmoeklund.service.PageService
import zio.*
import zio.http.html.*
import zio.http.html.Html.fromDomElement
import zio.http.{html as _, *}

case class Site[-R <: PageService](
    db: String,
    tabMenu: TabMenu,
    pages: List[Page[R, _, _]]
) {

  private def getPage(path: String) = 
    ZIO.fromOption(pages.find(f => f.path == path)).orElseFail(Response.status(Status.NotFound))

  private def setActive(page: Page[R, _, _]) = tabMenu.setActiveTab(Root / db / page.path)

  def htmlValue(page: Html): Html =
    html(htmxHead ++ body(div(classAttr := "container" :: Nil, tabMenu.htmlValue, page)))

  def httpValue: App[Map[String, R]] = Http.collectZIO[Request] {
    case request @ Method.GET -> Root / this.db / path / format =>
      format match {
        case "options" =>
          getPage(path).flatMap(page =>
            page.optionsList
              .mapBoth(
                e => Response.text(e.toString).withStatus(Status.InternalServerError),
                (p: Html) => htmlSnippet(p)
              )
          )

        case _ => ZIO.succeed(Response.status(Status.UnsupportedMediaType))
      }

    case Method.GET -> Root / this.db / path =>
      getPage(path).flatMap(page =>
        setActive(page)  
        page.tableList.foldZIO(
          e => {
            for {
              _ <- ZIO.logInfo(e.toString)
              response <- ZIO.succeed(Response.text(e.toString).withStatus(Status.InternalServerError))
            } yield response
          },
          (result: Html) => ZIO.succeed(Response.html(htmlValue(result)))
        )
      )

    case req @ Method.POST -> Root / this.db / path =>
      getPage(path).flatMap(page =>
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

    case Method.DELETE -> Root / this.db / path / id =>
      getPage(path).flatMap(page =>
        page
          .delete(id)
          .foldZIO(_ => ZIO.succeed(Response.status(Status.BadRequest)), _ => ZIO.succeed(Response.status(Status.Ok)))
      )
  }
}

object Site {
  def build(db: String) = {
    val pages = List(
      UsersPage("users", db),
      OrganizationsPage("organizations", db),
      RolesPage("roles", db),
      PermissionsPage("permissions", db)
    )
    val tabs = pages.map(p => Tab(p.path.capitalize, Root / db / p.path, false))
    Site(db, TabMenu(tabs), pages)
  }
}
