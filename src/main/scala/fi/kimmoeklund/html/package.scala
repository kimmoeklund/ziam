package fi.kimmoeklund.html

import fi.kimmoeklund.domain.{ErrorCode, FormError, Permission}
import zio.http.*
import zio.http.template.*
import zio.http.template.Attributes.PartialAttribute
import zio.prelude.Validation
import zio.{Chunk, ZIO}

import scala.util.Try

def htmxHead: Html = {
  head(
    Html.fromString("""<meta name="htmx-config" content='{"useTemplateFragments":true}'>"""),
    title("Permissions"),
    link(
      relAttr  := "stylesheet",
      hrefAttr := "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css"
    ),
    script(
      srcAttr := "https://unpkg.com/htmx.org@2.0.6",
      PartialAttribute(
        "integrityAttr"
      )                                   := "sha384-Akqfrbj/HpNVo8k11SXBb6TlBWmXXlYQrCSqEWmyKJe+hDm3Z/B2WVG4smwBkRVm",
      PartialAttribute("crossoriginAttr") := "anonymous"
    )
  )
}

def htmlSnippet(data: play.twirl.api.Html, status: Status = Status.Ok): Response =
  Response(
    status,
    Headers(Header.ContentType(MediaType.text.html).untyped),
    Body.fromCharSequence(data.body)
  )

def emptyHtml = Html.fromUnit(())

def selectOption(
    optsPath: String,
    name: String,
    selected: Seq[String] = Seq.empty,
    selectMultiple: Boolean = false
) =
  val baseElements = Chunk[Html](
    idAttr    := name,
    classAttr := "w-full min-w-0",
    nameAttr  := name,
    PartialAttribute("hx-get") := s"$optsPath${QueryParams(Map.from(Seq("selected" -> Chunk.fromIterable(selected)))).encode}",
//    PartialAttribute("hx-get") := "http://localhost:5080/colors",
    PartialAttribute("hx-trigger") := "load",
    //  PartialAttribute("hx-params")  := "none",
    // PartialAttribute("hx-disinherit")  := "*",
    PartialAttribute("hx-swap")   := "innerHTML",
    PartialAttribute("hx-target") := s"#$name",
    PartialAttribute("hx-select") := "*",
    option(valueAttr := "loading..")
  )
  val selectElements = if (selectMultiple) baseElements ++ Chunk[Html](multipleAttr := "true") else baseElements
  select(selectElements: _*)

extension (form: Form)
  def zioFromField(field: String): ZIO[Any, FormError, String] =
    ZIO.fromEither(form.get(field).flatMap(_.stringValue).toRight(FormError.Missing(field)))
