package fi.kimmoeklund.html

import fi.kimmoeklund.domain.ErrorCode
import fi.kimmoeklund.service.PageService
import zio.http.html.*
import zio.http.html.Html.fromDomElement
import zio.http.{html as _, *}
import zio.{ZIO, *}
import zio.http.html.Attributes.PartialAttribute

trait StaticHtml:
  def htmlValue: Html

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

trait Identifiable:
  val id: java.util.UUID

trait Page[-R <: PageService, A, B <: Identifiable](using htmlEncoder: HtmlEncoder[B]):

  val htmlId: String
  val path: String
  val db: String

  def mapToView: A => B
  def newFormRenderer: Html
  def tableList: ZIO[Map[String, R], ErrorCode, Html]
  def post(req: Request): ZIO[Map[String, R], ErrorCode, Html]
  def delete(id: String): ZIO[Map[String, R], ErrorCode, Unit]
  def optionsList: ZIO[Map[String, R], ErrorCode, Html]

  private def deleteButton(r: B) = 
      td(
        button(
          classAttr := "btn btn-danger" :: Nil,
          "Delete",
          PartialAttribute("hx-delete") := s"/$db/$path/${r.id}",
        ))

  def newResourceHtml(r: B): Html =
      tBody(
        PartialAttribute("hx-swap-oob") := s"beforeend:#$htmlId-table",
      tr(
        PartialAttribute("hx-target") := "this",
        PartialAttribute("hx-swap") := "delete",
        htmlEncoder.wrapValueWith(td, r),
        deleteButton(r)
      )
  )

  def htmlTable(args: Seq[A]) =
    table(
      classAttr := "table" :: Nil,
      tHead(
        tr(
          htmlEncoder.wrapParametersWith(th, _.capitalize)
        )
      ),
      tBody(
        idAttr := s"$htmlId-table",
        PartialAttribute("hx-swap-oob") := s"beforeend:#$htmlId-table",
        args
          .map(mapToView)
          .map(r =>
            tr(
              PartialAttribute("hx-target") := "this",
              PartialAttribute("hx-swap") := "delete",
              htmlEncoder.wrapValueWith(td, r),
              deleteButton(r)
            )
          )
      )
    ) ++ newFormRenderer
