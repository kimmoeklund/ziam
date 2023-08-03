package fi.kimmoeklund.html

import fi.kimmoeklund.domain.{ErrorCode, Permission}
import zio.http.{html as _, *}
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.prelude.Validation

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
    ),
  )
}

def htmlSnippet(data: Html, status: Status = Status.Ok): Response = 
  Response(
    status,
    Headers(Header.ContentType(MediaType.text.html).untyped),
    Body.fromCharSequence(data.encode)
  )
