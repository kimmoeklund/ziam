package fi.kimmoeklund.html

import fi.kimmoeklund.domain.ErrorCode
import zio.*
import zio.http.html.*
import zio.http.html.Html.fromDomElement
import zio.http.{html as _, *}

trait StaticHtml:
  def htmlValue: Html

trait Effects[R, T]:
  def getEffect: ZIO[Map[String, R], ErrorCode, List[T]]
  def postEffect(req: Request): ZIO[Map[String, R], ErrorCode, T]
  def deleteEffect(id: String): ZIO[Map[String, R], ErrorCode, Unit]

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
    li(
      classAttr := "nav" :: "nav-item" :: Nil,
      a(classAttr := "nav-link" :: "active" :: Nil, href := path.toString, name)
    )

case class Tab(name: String, path: Path, var active: Boolean) extends MenuItem:
  def htmlValue: Html =
    li(classAttr := "nav" :: "nav-item" :: Nil, a(classAttr := "nav-link" :: Nil, href := path.toString, name))

case class TabMenu(items: List[Tab]) extends Menu:
  def htmlValue: Html = ul(classAttr := "nav" :: "nav-tabs" :: Nil, items.map(_.htmlValue).reduce(_ ++ _))
  def setActiveTab(path: Path): Unit = {
    items.map {
      case item @ Tab(_, p, _) if p == path    => item.active = true
      case item @ Tab(_, p, true) if p != path => item.active = false
      case item @ Tab(_, _, _)                 => item
    }
  }

case class SimplePage[R, T](path: String, functions: Effects[R, T] & Renderer[T]):

  def htmlValue(contentArgs: List[T]): Html = functions.htmlTable(contentArgs)

//
