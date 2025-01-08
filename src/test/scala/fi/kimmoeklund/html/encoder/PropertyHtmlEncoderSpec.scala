package fi.kimmoeklund.html.encoder

import zio.test.*
import java.util.UUID
import fi.kimmoeklund.html.encoder.PropertyHtmlEncoder
import scala.annotation.Annotation
import fi.kimmoeklund.html.encoder.TemplateInput
import play.twirl.api.Html

import scala.annotation.Annotation
import fi.kimmoeklund.html.encoder.ErrorMsg
import fi.kimmoeklund.html.encoder.errorMsgs
import play.twirl.api.HtmlFormat

final class testAnnotation extends Annotation
final class testAnnotationWithValue(val data: String) extends Annotation

enum TestEnum {
  case Foo
  case Bar(happy: String)
}

case class AnnotatedData(@testAnnotation val data: String, @testAnnotationWithValue("value") val data1: Int)
case class AnnotatedOpt(
    @testAnnotation val data: Option[String],
    @testAnnotationWithValue("value") val data1: Option[Int]
)
case class Data(val data: String, val data1: Int)
case class DataOpt(val data: Option[String])

given PropertyHtmlEncoder[TestEnum] = PropertyHtmlEncoder.derived[TestEnum]
given PropertyHtmlEncoder[DataOpt] = PropertyHtmlEncoder.derived[DataOpt]
given PropertyHtmlEncoder[Data] = PropertyHtmlEncoder.derived[Data]
given PropertyHtmlEncoder[AnnotatedData] = PropertyHtmlEncoder.derived[AnnotatedData]
given PropertyHtmlEncoder[AnnotatedOpt] = PropertyHtmlEncoder.derived[AnnotatedOpt]

object ValuePropertyHtmlEncoderSpec extends ZIOSpecDefault:
  import fi.kimmoeklund.html.encoder.given
  def mapAnnotations(in: TemplateInput[String]) = 
          in.annotations
            .map({
              case a: testAnnotation          => "testAnnotation"
              case b: testAnnotationWithValue => b.data
            }).mkString(" ")
  val tdTemplate = (in: TemplateInput[String]) => HtmlFormat.raw(s"<td>${in.value.getOrElse(in.paramName)}</td>")  
  val tdParamsTemplate = (in: TemplateInput[String]) =>  HtmlFormat.raw(s"<td>${in.paramName}</td>")
  val annotationTemplate = (in: TemplateInput[String]) => HtmlFormat.raw(s"<td class=\"${mapAnnotations(in)}\">${in.value.getOrElse(in.paramName)}</td>")
  val data = Data("value", 42)

  val htmlEncoderSuite = suite("PropertyHtmlEncoder")(
    test("it should encode enum name to <td>") {
      val result = PropertyHtmlEncoder[TestEnum].encode(tdParamsTemplate).map(_.body).mkString("")
      assertTrue(result == "<td>TestEnum</td>")
    },
    test("it should encode Int value parameter name as \"\"") {
      val foo = PropertyHtmlEncoder[Int].encode(tdParamsTemplate)
      assertTrue(foo.map(_.body).mkString("") == "<td></td>")
    },
    test("it should encode DataOpt parameters to <td>") {
      val result = PropertyHtmlEncoder[DataOpt].encode(tdParamsTemplate).map(_.body).mkString("")
      assertTrue(result == "<td>data</td>")
    },
    test("it should encode annotated case class params to <td>") {
      val result = PropertyHtmlEncoder[AnnotatedData].encode(annotationTemplate).map(_.body).mkString("")
      assertTrue(result == "<td class=\"testAnnotation\">data</td><td class=\"value\">data1</td>")
    },
  )

  val errorMsgSuite = suite("ErrorMsg extension")(test("it should filter error messages based on param name") {
      val someErrors = Some(Seq(ErrorMsg("foo", "bar"), ErrorMsg("foo", "baz"), ErrorMsg("bar", "baz")))
      assertTrue(someErrors.errorMsgs("foo") == Some(Seq("bar", "baz")))
      assertTrue(someErrors.errorMsgs("expect_none") == None)
    })

  override def spec = htmlEncoderSuite + errorMsgSuite 
