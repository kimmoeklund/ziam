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
final class inputNumber extends Annotation
final class inputPassword extends Annotation
final class inputSelectOptions(val path: String, val name: String, val selectMultiple: Boolean = false)
    extends Annotation

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
      case i: inputNumber => 
        Html.fromDomElement(
          input(
            nameAttr := value,
            idAttr := value,
            Tailwind.formInput,
            typeAttr := "number"
          )
        )
      case _                     => Html.fromUnit(())
    }

  private val formTemplate = (value: String, mapperOutput: Seq[Html]) =>
    Html.fromDomElement(
      div(
        label(value.capitalize, forAttr := value, Tailwind.formLabel),
        div(
          classAttr := "mt-2" :: Nil,
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

  val path: String
  val db: String

  def mapToView: A => B
  def listItems: ZIO[Map[String, R], ErrorCode, Seq[A]]
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
      PartialAttribute("hx-swap-oob") := "beforeend:#resource-table",
      tr(
        PartialAttribute("hx-target") := "this",
        PartialAttribute("hx-swap") := "delete",
        htmlEncoder.encodeValues(tdTemplate, r).fold(Html.fromUnit(()))(_ ++ _),
        deleteButton(r)
      )
    )

  def tableHeaders =
    htmlEncoder.encodeParams(thTemplate) 

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
