package fi.kimmoeklund.html

import fi.kimmoeklund.domain.{ErrorCode, RoleId, UserId}
import fi.kimmoeklund.html.ElementTemplate
import fi.kimmoeklund.html.forms.formTemplate
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.{html as _, *}
import zio.{ZIO, *}

import scala.annotation.Annotation
import fi.kimmoeklund.service.Repositories

final class inputEmail extends Annotation
final class inputNumber extends Annotation
final class inputPassword extends Annotation
final class inputHidden extends Annotation
final class inputSelectOptions(val path: String, val name: String, val selectMultiple: Boolean = false)
    extends Annotation

trait Identifiable:
  val id: java.util.UUID | RoleId | UserId 
end Identifiable

trait LoginPage[R, A]:
  val loginPath: String
  val logoutPath: String
  def doLogin(request: Request): ZIO[Map[String, R], ErrorCode, A]
  def showLogin: Html
end LoginPage

trait Page[-R, A, B <: Identifiable](using htmlEncoder: HtmlEncoder[B]):

  val path: String
  val db: String

  def mapToView: A => B
  def listItems: ZIO[Map[String, R], ErrorCode, Seq[A]]
  def upsertResource(req: Request): ZIO[Map[String, R], ErrorCode, Html]
  def delete(id: String): ZIO[Map[String, R], ErrorCode, Unit]
  def optionsList(selected: Option[Seq[String]] = None): ZIO[Map[String, R], ErrorCode, Html]

  protected val tdTemplate: ElementTemplate = input => Html.fromDomElement(td(input.value.getOrElse(input.paramName), Tailwind.td))
  protected val thTemplate: ElementTemplate = input => Html.fromDomElement(th(input.paramName, Tailwind.th))

  protected def deleteButton(r: B) =
    td(
      button(
        classAttr := "btn" :: Nil,
        PartialAttribute("hx-delete") := s"/$db/$path/${r.id}",
        Dom.text(Icons.solidMicroDelete),
        span("Delete")
      ),
      button(
        classAttr := "btn btn-danger" :: Nil,
        PartialAttribute("onclick") := s"loadResource('${r.id}')",
        Dom.text(Icons.solidMicroEdit),
        span("Edit")
      )
    )


  def tableHeaders =
    htmlEncoder.encodeParams(thTemplate)

  def tableRows = listItems
    .map(_.map(mapToView))
    .map(args =>
      args.map(r =>
        tr(
          PartialAttribute("hx-target") := "this",
          PartialAttribute("hx-swap") := "delete",
          htmlEncoder.encodeValues(tdTemplate, r).fold(emptyHtml)(_ ++ _),
          deleteButton(r)
        )
      )
    )
end Page
