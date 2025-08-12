package fi.kimmoeklund.html

import fi.kimmoeklund.domain.{CrudResource, ErrorCode, Identifiable}
import fi.kimmoeklund.html.encoder.*
import fi.kimmoeklund.html.forms.{UserForm, formTemplate}
import fi.kimmoeklund.repository.{QuillCtx, Repository}
import fi.kimmoeklund.templates.html.{
  crud_table,
  crud_table_row,
  crud_table_row_buttons,
  crud_table_header,
  crud_page_new_response
}
import play.twirl.api.{Html, HtmlFormat}
import zio.http.*
import zio.http.template.Attributes.PartialAttribute
import zio.http.template.{td, th, div, idAttr}
import zio.{Cause, Chunk, IO, URIO, ZIO}
import scala.util.Try
import fi.kimmoeklund.domain.FormWithErrors
import fi.kimmoeklund.domain.FormError
import fi.kimmoeklund.templates.html.crud_table_actions_header

sealed trait UpsertResult[T]:
  val entity: T
end UpsertResult

case class UpdatedEntity[T](entity: T) extends UpsertResult[T]
case class CreatedEntity[T](entity: T) extends UpsertResult[T]

trait CrudPage[R, Entity <: CrudResource[Form], View <: Identifiable, Form] extends Page[R]:

  private def htmlForm(resource: Option[Form], errors: Option[Seq[ErrorMsg]] = None, idAttr: Option[String] = None) =
    (resource match
      case Some(r) => HtmlFormat.fill(formValueEncoder.encode(formTemplate(site), r, errors))
      case None    => HtmlFormat.fill(formPropertyEncoder.encode(formTemplate(site)))
    )
  private def tableRow(v: View) = HtmlFormat.fill(
    Seq(
      HtmlFormat.fill(viewValueEncoder.encode(crud_table_row.apply, v)),
      crud_table_row_buttons(this.path.encode, v.id.toString)
    )
  )
  private def formAndRow(r: UpsertResult[Entity]) =
    val view = mapToView(r.entity)
    crud_page_new_response(
      htmlForm(Some(r.entity.form), None),
      this.tableRow(view),
      r match
        case e: UpdatedEntity[Entity] => Some(view.id.toString)
        case e: CreatedEntity[Entity] => None
    )

  private def formWithErrors(form: Option[Form], errors: Seq[ErrorMsg]) =
    crud_page_new_response(htmlForm(form, Some(errors)), HtmlFormat.empty, None)

  def parseForm(using FormDecoder[Form])(request: Request) = request.body.asURLEncodedForm
    .flatMap(form => ZIO.fromOption(FormDecoder[Form].decode(form)))
    .mapError(_ => FormWithErrors[Form](List(FormError.ProcessingFailed("System error, unable to parse form")), None))

  def upsert(using QuillCtx)(request: Request): URIO[R, Html] =
    upsertResource(request)
      .fold(
        error => formWithErrors(error.form, error.errors.map(errorHandler)),
        resource => formAndRow(resource)
      )

  def delete(using QuillCtx)(id: String): URIO[R, Html] =
    deleteInternal(id)
      .map(_ => HtmlFormat.empty)
      .catchAll(e => ZIO.succeed(Html.apply(s"div(error creating resource: $e")))

  def getAsForm(using QuillCtx)(idOpt: Option[String]): URIO[R, Html] = idOpt match {
    case Some(id) =>
      get(id)
        .map(entity => htmlForm(Some(entity.form)))
        .catchAll(e => ZIO.succeed(Html(s"div(error rendering resource form $e")))
    case None =>
      ZIO.succeed(htmlForm(None, None))
  }

  def renderAsTable(using QuillCtx): URIO[R, Html] =
    (for {
      items   <- listItems
      rows    <- ZIO.fromTry(Try(items.map(i => tableRow(mapToView(i)))))
      headers <- ZIO.fromTry(Try(viewPropertyEncoder.encode(crud_table_header.apply)))
      tableHtml <- ZIO.fromTry(
        Try(
          crud_table(
            HtmlFormat.fill(headers.appended(HtmlFormat.raw(crud_table_actions_header().body))),
            items.map(mapToView(_).id.toString).zip(rows)
          )
        )
      )
    } yield tableHtml).catchAll(e =>
      ZIO.logError(e.toString) *> ZIO.succeed(Html(s"<div>error fetching data: ${e}</div>"))
    )

  def renderAsOptions(using QuillCtx)(selected: Chunk[String]): ZIO[R, ErrorMsg, Html]

  // every implementation must provide given instances
  protected val formPropertyEncoder: PropertyHtmlEncoder[Form]
  protected val formValueEncoder: ValueHtmlEncoder[Form]
  protected val viewPropertyEncoder: PropertyHtmlEncoder[View]
  protected val viewValueEncoder: ValueHtmlEncoder[View]
  protected val site = path.take(2).toString

  // every page must implement
  protected def errorHandler: ErrorCode => ErrorMsg
  protected def mapToView: Entity => View
  protected def listItems(using QuillCtx): ZIO[R, ErrorCode, Seq[Entity]]
  protected def upsertResource(using QuillCtx)(req: Request): ZIO[R, FormWithErrors[Form], UpsertResult[Entity]]
  protected def deleteInternal(using QuillCtx)(id: String): ZIO[R, ErrorMsg, Unit]
  protected def get(using QuillCtx)(id: String): ZIO[R, ErrorMsg, Entity]
