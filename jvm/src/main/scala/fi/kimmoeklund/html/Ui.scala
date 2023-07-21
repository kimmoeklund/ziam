package fi.kimmoeklund.html

import fi.kimmoeklund.domain.ErrorCode
import fi.kimmoeklund.service.UserRepository
import zio.*
import zio.http.Status.InternalServerError
import zio.http.{html as _, *}
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement

import java.util.UUID

trait StaticHtml:
  def htmlValue: Html

trait Effects[R,T]:
  def getEffect: ZIO[R, Throwable, List[T]]
  def postEffect(req: Request): ZIO[R, Option[Nothing] | ErrorCode | Throwable , T]
  def deleteEffect(id: String): ZIO[R, Option[Nothing] | Throwable, Unit]

trait Renderer[T]:
  def htmlTable(args: List[T]): Html
  def postResult(item: T): Html
  def optionsList(args: List[T]): Html

trait Menu extends StaticHtml:
  val items: List[MenuItem]

trait MenuItem extends StaticHtml:
  val path: Path

case class ActiveTab(name: String, path: Path) extends MenuItem:
  def htmlValue: Html =
    li(classAttr := "nav" :: "nav-item" :: Nil, a(classAttr := "nav-link" :: "active" :: Nil, href := path.toString, name))

case class Tab(name: String, path: Path) extends MenuItem:
  def htmlValue: Html = li(classAttr := "nav" :: "nav-item" :: Nil, a(classAttr := "nav-link" :: Nil, href := path.toString, name))

case class TabMenu(items: List[Tab | ActiveTab]) extends Menu:
  def htmlValue: Html = ul(classAttr := "nav" :: "nav-tabs" :: Nil, items.map(_.htmlValue).reduce(_ ++ _))
  def setActiveTab(newActiveTab: Tab): TabMenu = {
    val updatedItems = items.map {
      case ActiveTab(name, path) => Tab(name, path)
      case item @ Tab(name, path) if item == newActiveTab => ActiveTab(name, path)
      case item => item
    }
    TabMenu(updatedItems)
  }

case class SimplePage[R,T](path: Path, menu: Menu, functions: Effects[R,T] & Renderer[T]):

  def htmlValue(contentArgs: List[T]): Html =
    html(htmxHead ++ body(div(classAttr := "container" :: Nil, menu.htmlValue, functions.htmlTable(contentArgs))))

  def httpValue: HttpApp[R, Nothing] = Http.collectZIO[Request]:
    case Method.GET -> this.path / format => functions.getEffect.foldZIO(e => ZIO.succeed(Response.text(e.getMessage).withStatus(Status.InternalServerError)),
      (result: List[T]) => format match
          case "options" => ZIO.succeed(htmlSnippet(functions.optionsList(result)))
          case _ => ZIO.succeed(Response.html(htmlValue(result))))

    case Method.GET -> this.path => functions.getEffect.foldZIO(e => {
      for {
        _ <- ZIO.logInfo(e.getMessage)
        _ <- ZIO.logInfo(e.getStackTrace.mkString("\n"))
        response <- ZIO.succeed(Response.text(e.getMessage).withStatus(Status.InternalServerError))
      } yield response
    },
      (result: List[T]) => ZIO.succeed(Response.html(htmlValue(result))))

      case req @ Method.POST -> this.path => functions.postEffect(req).foldCauseZIO((error: Cause[Option[Nothing] | ErrorCode | Throwable]) => { 
      error match
      {
        case c: Cause[_] => ZIO.succeed(Response.text(s"errors: ${c.failures.map(e => e.toString).mkString("\n")}\ndefects: ${c.defects.map(d => d.getMessage()).mkString("\n")}").withStatus(Status.BadRequest))
        case _ => ZIO.succeed(Response.text("error not yet available").withStatus(Status.InternalServerError))
      }
    },
      p => ZIO.succeed(htmlSnippet(functions.postResult(p)).addHeader("HX-Trigger-After-Swap", "resetAndFocusForm")))

    case Method.DELETE -> this.path / id => functions.deleteEffect(id).foldZIO(_ => ZIO.succeed(Response.status(Status.BadRequest)),
      _ => ZIO.succeed(Response.status(Status.Ok)))


