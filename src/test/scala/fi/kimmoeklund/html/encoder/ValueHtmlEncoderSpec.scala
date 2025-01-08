package fi.kimmoeklund.html.encoder

import zio.test.*
import java.util.UUID
import fi.kimmoeklund.html.encoder.ValueHtmlEncoder
import scala.annotation.Annotation
import fi.kimmoeklund.html.encoder.TemplateInput
import play.twirl.api.Html

import scala.annotation.Annotation
import fi.kimmoeklund.html.encoder.ErrorMsg
import fi.kimmoeklund.html.encoder.errorMsgs
import play.twirl.api.HtmlFormat

given ValueHtmlEncoder[TestEnum] = ValueHtmlEncoder.derived[TestEnum]
given ValueHtmlEncoder[DataOpt] = ValueHtmlEncoder.derived[DataOpt]
given ValueHtmlEncoder[Data] = ValueHtmlEncoder.derived[Data]
given ValueHtmlEncoder[AnnotatedData] = ValueHtmlEncoder.derived[AnnotatedData]
given ValueHtmlEncoder[AnnotatedOpt] = ValueHtmlEncoder.derived[AnnotatedOpt]

object ValueValueHtmlEncoderSpec extends ZIOSpecDefault:
  import fi.kimmoeklund.html.encoder.given
  def mapAnnotations(in: TemplateInput[String]) = 
          in.annotations
            .map({
              case a: testAnnotation          => "testAnnotation"
              case b: testAnnotationWithValue => b.data
            }).mkString(" ")
  val tdTemplate = (in: TemplateInput[String]) => HtmlFormat.raw(s"<td>${in.value.getOrElse(in.value)}</td>")  
  val tdParamsTemplate = (in: TemplateInput[String]) =>  HtmlFormat.raw(s"<td>${in.paramName}</td>")
  val annotationTemplate = (in: TemplateInput[String]) => HtmlFormat.raw(s"<td class=\"${mapAnnotations(in)}\">${in.value.getOrElse(in.paramName)}</td>")
  val data = Data("value", 42)
  val data2 = Data("value2", 43)

  val htmlEncoderSuite = suite("ValueHtmlEncoder")(
    test("it should encode case class values to <td>") {
      assertTrue(
        ValueHtmlEncoder[Data]
          .encode(tdTemplate, data)
          .map(_.body)
          .mkString(
            ""
          ) == "<td>value</td><td>42</td>"
      )
    },
    test("it should encode enum value to <td>") {
      val result = ValueHtmlEncoder[TestEnum]
        .encode(tdTemplate, TestEnum.Foo)
        .map(_.body)
        .mkString("")
      assertTrue(
        result == "<td>Foo</td>"
      )
    },
    test("it should encode enum with value to <td>") {
      val result = ValueHtmlEncoder[TestEnum]
        .encode(tdTemplate, TestEnum.Bar("isHappy"))
        .map(_.body)
        .mkString("")
      assertTrue(
        result == "<td>Bar(isHappy)</td>"
      )
    })

  override def spec = htmlEncoderSuite  
