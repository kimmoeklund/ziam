package fi.kimmoeklund.html

import fi.kimmoeklund.service.UserRepository
import zio.*
import zio.http.{html as _, *}
import zio.http.html.*
import zio.http.html.Html.fromDomElement

import java.util.UUID

trait StaticHtml:
  def htmlValue: Html

trait Effects[R,T]:
  def listRenderer(args: List[T]): Html
  def postItemRenderer(item: T): Html
  def getEffect: ZIO[R, Throwable, List[T]]
  def postEffect(req: Request): ZIO[R, Option[Nothing] | Throwable , T]
  def deleteEffect(id: String): ZIO[R, Option[Nothing] | Throwable, Unit]

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

case class SimplePage[R,T](path: Path, menu: Menu, effects: Effects[R,T]):
  def htmlValue(contentArgs: List[T]): Html =
    html(htmxHead ++ body(div(classAttr := "container" :: Nil, menu.htmlValue, effects.listRenderer(contentArgs)))) ///content(contentArgs))))

  def httpValue: HttpApp[R, Nothing] = Http.collectZIO[Request]:
    case Method.GET -> path => effects.getEffect.foldZIO(_ => ZIO.succeed(Response.status(Status.InternalServerError)),
      (result: List[T]) => ZIO.succeed(Response.html(htmlValue(result))))

    case req @ Method.POST -> path => effects.postEffect(req).foldZIO(
      _ => ZIO.succeed(Response.status(Status.BadRequest)),
      p => ZIO.succeed(htmlSnippet(effects.postItemRenderer(p)).addHeader("HX-Trigger-After-Swap", "myEvent")))

    case Method.DELETE -> path / id => effects.deleteEffect(id).foldZIO(_ => ZIO.succeed(Response.status(Status.BadRequest)),
      _ => ZIO.succeed(Response.status(Status.Ok)))


