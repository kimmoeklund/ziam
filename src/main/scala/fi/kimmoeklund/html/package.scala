package fi.kimmoeklund.html

import fi.kimmoeklund.domain.{ErrorCode, FormError, Permission}
import zio.http.*
import zio.http.template.*
import zio.http.template.Attributes.PartialAttribute
import zio.prelude.Validation
import zio.{Chunk, ZIO}

import scala.util.Try

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
  // using Dom.raw because PartialAttributes are htmlencoded which breaks the url encoding
  Dom.raw(s"""
  <select id=${name} name=${name} class="w-full min-w-o" hx-get="$optsPath${QueryParams(Map.from(Seq("selected" -> Chunk.fromIterable(selected)))).encode}" hx-trigger="load" hx-swap="innerHTML" hx-target="#${name}" hx-select="*" ${if selectMultiple then "multiple" else ""}>
  </select>
  """)

extension (form: Form)
  def zioFromField(field: String): ZIO[Any, FormError, String] =
    ZIO.fromEither(form.get(field).flatMap(_.stringValue).toRight(FormError.Missing(field)))
