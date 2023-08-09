package fi.kimmoeklund.html

import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import scala.collection.mutable.ArrayBuffer
import zio.Chunk

object Htmx:

  def selectOption(optsPath: String, name: String, selectMultiple: Boolean = false) =
    val attributes = Chunk(
      idAttr := s"$name-select",
      classAttr := "form-select" :: Nil,
      nameAttr := name,
      PartialAttribute("hx-get") := optsPath,
      PartialAttribute("hx-trigger") := "revealed",
      PartialAttribute("hx-params") := "none",
      PartialAttribute("hx-target") := s"#$name-select",
        PartialAttribute("hx-swap") := "innerHTML"
    ) ++ (if (selectMultiple) then Chunk[Html](multipleAttr := "multiple") else Chunk[Html]())
    Html.fromDomElement(select(attributes:_*))
