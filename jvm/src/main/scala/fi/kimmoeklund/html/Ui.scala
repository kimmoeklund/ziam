package fi.kimmoeklund.html

import fi.kimmoeklund.domain.ErrorCode
import fi.kimmoeklund.service.PageService
import zio.http.html.*
import zio.http.html.Html.fromDomElement
import zio.http.{html as _, *}
import zio.{ZIO, *}
import zio.http.html.Attributes.PartialAttribute
import org.scalafmt.config.DanglingParentheses.Exclude.`class`
import scala.annotation.Annotation

final class inputEmail extends Annotation
final class inputPassword extends Annotation
final class inputSelectOptions(val path: String, val name: String, val selectMultiple: Boolean = false)
    extends Annotation

trait StaticHtml:
  def htmlValue: Html

trait Menu extends StaticHtml:
  val items: List[MenuItem]

trait MenuItem extends StaticHtml:
  val path: Path

case class Tab(name: String, path: Path, var active: Boolean) extends MenuItem:
  def htmlValue: Html =
    li(
      classAttr := "nav" :: "nav-item" :: Nil,
      a(classAttr := "nav-link" :: (if (active) then "active" :: Nil else Nil), href := path.toString, name)
    )

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

trait LoginPage[R, A]:
  val loginPath: String
  val logoutPath: String
  def doLogin(request: Request): ZIO[Map[String, R], ErrorCode, A]
  def showLogin: Html

trait NewResourceForm[A](using htmlEncoder: HtmlEncoder[A]):
  val db: String
  val path: String

  private val inputTypeMapper = (annotation: Any, value: String) =>
    println("mapping: " + annotation.toString)
    annotation match {
      case a: inputEmail =>
        Html.fromDomElement(
          input(
            nameAttr := value,
            idAttr := value,
            Tailwind.formInput,
            typeAttr := "email"
          )
        )
      case a: inputPassword =>
        Html.fromDomElement(
          input(
            nameAttr := value,
            idAttr := value,
            Tailwind.formInput,
            typeAttr := "password"
          )
        )
      case o: inputSelectOptions => Htmx.selectOption(s"${o.path}", o.name, o.selectMultiple)
      case _                     => Html.fromUnit(())
    }

  private val formTemplate = (value: String, mapperOutput: Seq[Html]) =>
    Html.fromDomElement(
      div(
        label(value.capitalize, forAttr := value, Tailwind.formLabel),
        div(
          classAttr := "mt-2" :: Nil,
          /// ongelma on ett' osa kentist' on select ja osa input
          if mapperOutput.length > 0 then mapperOutput.fold(Html.fromUnit(()))(_ ++ _)
          else
            input(
              nameAttr := value,
              idAttr := value,
              Tailwind.formInput,
              typeAttr := "text"
            )
        )
      )
    )

  def htmlForm = htmlEncoder.encodeParams(formTemplate, inputTypeMapper)
end NewResourceForm

trait Page[R, A, B <: Identifiable](using htmlEncoder: HtmlEncoder[B]):

  val htmlId: String
  val path: String
  val db: String

  def mapToView: A => B
  def newFormRenderer: Html
  def listItems: ZIO[Map[String, R], ErrorCode, Seq[A]]
  def tableList: ZIO[Map[String, R], ErrorCode, Html]
  def post(req: Request): ZIO[Map[String, R], ErrorCode, Html]
  def delete(id: String): ZIO[Map[String, R], ErrorCode, Unit]
  def optionsList: ZIO[Map[String, R], ErrorCode, Html]

  private val tdTemplate = (value: String) => Html.fromDomElement(td(value, Tailwind.td))
  private val thTemplate = (value: String, mapperOutput: Seq[Html]) => Html.fromDomElement(th(value, Tailwind.th))

  private def deleteButton(r: B) =
    td(
      button(
        classAttr := "btn btn-danger" :: Nil,
        "Delete",
        PartialAttribute("hx-delete") := s"/$db/$path/${r.id}"
      )
    )

  def newResourceHtml(r: B): Html =
    tBody(
      PartialAttribute("hx-swap-oob") := s"beforeend:#$htmlId-table",
      tr(
        PartialAttribute("hx-target") := "this",
        PartialAttribute("hx-swap") := "delete",
        htmlEncoder.encodeValues(tdTemplate, r).fold(Html.fromUnit(()))(_ ++ _),
        deleteButton(r)
      )
    )

  def tableHeaders =
    htmlEncoder.encodeParams(thTemplate) // htmlEncoder(th, _.capitalize, Chunk(scopeAttr := "col", Tailwind.thClass))

  // todo: double mapping
  def tableRows = listItems
    .tap(a => ZIO.logInfo(a.map(_.toString).mkString(",")))
    .map(_.map(mapToView))
    .map(args =>
      args.map(r =>
        tr(
          PartialAttribute("hx-target") := "this",
          PartialAttribute("hx-swap") := "delete",
          htmlEncoder.encodeValues(tdTemplate, r).fold(Html.fromUnit(()))(_ ++ _),
          deleteButton(r)
        )
      )
    )

  def htmlTable(args: Seq[A]) = ???
