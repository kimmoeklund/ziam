package fi.kimmoeklund.html

import fi.kimmoeklund.domain.ErrorCode
import fi.kimmoeklund.service.PageService
import zio.http.html.*
import zio.http.html.Html.fromDomElement
import zio.http.{html as _, *}
import zio.{ZIO, *}

trait StaticHtml:
  def htmlValue: Html

trait Effects[R]:
  def tableList: ZIO[Map[String, R], ErrorCode, Html]
  def post(req: Request): ZIO[Map[String, R], ErrorCode, Html]
  def delete(id: String): ZIO[Map[String, R], ErrorCode, Unit]
  // def postResult(item: T): Html
  // def htmlTable(args: List[T]): Html
  def optionsList: ZIO[Map[String, R], ErrorCode, Html]

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

trait Page[-R <: PageService]:
  val path: String
  val db: String

  def tableList: ZIO[Map[String, R], ErrorCode, Html]
  def post(req: Request): ZIO[Map[String, R], ErrorCode, Html]
  def delete(id: String): ZIO[Map[String, R], ErrorCode, Unit]
  // def postResult(item: T): Html
  // def htmlTable(args: List[T]): Html
  def optionsList: ZIO[Map[String, R], ErrorCode, Html]

//case class SimplePage[R](path: String, functions: Effects[R])
