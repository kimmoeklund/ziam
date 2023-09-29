package fi.kimmoeklund.html

import fi.kimmoeklund.html.Page
import fi.kimmoeklund.html.Identifiable
import fi.kimmoeklund.service.{ Repositories, Resources, Forms }
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import fi.kimmoeklund.html.forms.formTemplate
import zio.ZIO
import fi.kimmoeklund.domain.CrudResource
import fi.kimmoeklund.domain.ErrorCode

trait CrudPage[R, A, B <: Identifiable, C] extends Page[R, CrudResource[A, C], B]:
  val attributes = (idAttr := "resource-table-body") //++ (PartialAttribute("hx-swap-oob") := "beforeend");

  private def tableRow(r: CrudResource[A,C])(using htmlEncoder: HtmlEncoder[C]) =  
      Html.fromDomElement(tBody(attributes,
      tr(
        PartialAttribute("hx-target") := "this",
        PartialAttribute("hx-swap") := "delete",
        htmlEncoder.encodeValues(tdTemplate, r.form).fold(emptyHtml)(_ ++ _),
        deleteButton(mapToView(r))
      ))
  )

  def emptyForm: C

  def htmlForm(resource: Option[C], errors: Option[Seq[ErrorMsg]] = None)(using htmlEncoder: HtmlEncoder[C]) = (resource match
      case Some(r) => htmlEncoder.encodeValues(formTemplate, r, errors)
      case None => htmlEncoder.encodeParams(formTemplate, "", Seq.empty, Some(this.emptyForm))).fold(emptyHtml)(_ ++ _)

  def get(id: String): ZIO[Map[String, R], ErrorCode, CrudResource[A, C]]

  def newResourceHtml(r: CrudResource[A, C])(using formEncoder: HtmlEncoder[C]) = Html.fromDomElement(div(idAttr := "form-response", htmlForm(Some(r.form), None))) ++ this.tableRow(r)

