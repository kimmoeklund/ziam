package fi.kimmoeklund.html

import fi.kimmoeklund.domain.{ErrorCode, Permission}
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.html.*
import zio.http.{html as _, *}
import zio.prelude.Validation
import zio.Chunk
import zio.ZIO
import fi.kimmoeklund.domain.FormError
import scala.util.Try

def htmxHead: Dom = {
  head(
    Html.fromString("""<meta name="htmx-config" content='{"useTemplateFragments":true}'>"""),
    title("Permissions"),
    link(
      relAttr := "stylesheet",
      hrefAttr := "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css"
    ),
    script(
      srcAttr := "https://unpkg.com/htmx.org@1.9.3",
      PartialAttribute(
        "integrityAttr"
      ) := "sha384-lVb3Rd/Ca0AxaoZg5sACe8FJKF0tnUgR2Kd7ehUOG5GCcROv5uBIZsOqovBAcWua",
      PartialAttribute("crossoriginAttr") := "anonymous"
    )
  )
}

def htmlSnippet(data: Html, status: Status = Status.Ok): Response =
  Response(
    status,
    Headers(Header.ContentType(MediaType.text.html).untyped),
    Body.fromCharSequence(data.encode)
  )

def emptyHtml = Html.fromUnit(())

def selectOption(
    optsPath: String,
    name: String,
    selected: Option[Seq[String]] = None,
    selectMultiple: Boolean = false
) =
  val selectedQueryParams = selected.map(_.map(v => "selected" -> Chunk(v))).map(a => QueryParams(a: _*)).map(_.encode)
  val attributes = Chunk(
    idAttr := name,
    classAttr := "form-select" :: Nil,
    nameAttr := name,
    PartialAttribute("hx-get") := s"$optsPath${selectedQueryParams.getOrElse("")}",
    PartialAttribute("hx-trigger") := "load",
    PartialAttribute("hx-params") := "none",
    PartialAttribute("hx-target") := s"#$name",
    PartialAttribute("hx-swap") := "innerHTML"
  ) ++ (if (selectMultiple) then Chunk[Html](multipleAttr := "multiple") else Chunk[Html]())
  select(attributes: _*)

extension (form: Form)
  def zioFromField(field: String): ZIO[Any, FormError, String] = ZIO.fromEither(form.get(field).map(_.stringValue).map(_.get).toRight(FormError.Missing(field)))
