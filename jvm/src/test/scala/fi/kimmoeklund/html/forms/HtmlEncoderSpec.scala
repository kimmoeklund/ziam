package fi.kimmoeklund.html.forms

import zio.test.*
import zio.http.html.*
import fi.kimmoeklund.domain.User
import java.util.UUID
import fi.kimmoeklund.domain.Organization
import fi.kimmoeklund.html.HtmlEncoder
import fi.kimmoeklund.domain.LoginType
import fi.kimmoeklund.html.pages.UserView
import scala.annotation.Annotation
import fi.kimmoeklund.html.TemplateInput

import scala.annotation.Annotation
import fi.kimmoeklund.html.ErrorMsg
import fi.kimmoeklund.html.errorMsgs
import com.outr.scalapass.Argon2.i

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

given HtmlEncoder[TestEnum] = HtmlEncoder.derived[TestEnum]
given HtmlEncoder[LoginType] = HtmlEncoder.derived[LoginType]
given HtmlEncoder[DataOpt] = HtmlEncoder.derived[DataOpt]
given HtmlEncoder[Data] = HtmlEncoder.derived[Data]
given HtmlEncoder[AnnotatedData] = HtmlEncoder.derived[AnnotatedData]
given HtmlEncoder[AnnotatedOpt] = HtmlEncoder.derived[AnnotatedOpt]

object HtmlEncoderSpec extends ZIOSpecDefault:
  val tdTemplate = (in: TemplateInput) => Html.fromDomElement(td(in.value.getOrElse(in.paramName)))
  val tdParamsTemplate = (in: TemplateInput) => Html.fromDomElement(td(in.paramName))
  val annotationTemplate = (in: TemplateInput) =>
    Html.fromDomElement(
      td(
        in.value.getOrElse(in.paramName),
        classAttr :=
          in.annotations
            .map({
              case a: testAnnotation          => "testAnnotation"
              case b: testAnnotationWithValue => b.data
            })
            .toList
      )
    )

  val data = Data("value", 42)

  val htmlEncoderSuite = suite("HtmlEncoder")(
    test("it should encode case class values to <td>") {
      assertTrue(
        HtmlEncoder[Data]
          .encodeValues(tdTemplate, data)
          .map(_.encode)
          .mkString(
            ""
          ) == "<td>value</td><td>42</td>"
      )
    },
    test("it should encode case class parameter names to <td>") {
      val result = HtmlEncoder[Data]
        .encodeParams(tdParamsTemplate)
        .map(_.encode)
        .mkString("")
      assertTrue(
        result == "<td>data</td><td>data1</td>"
      )
    },
    test("it should encode enum value to <td>") {
      val result = HtmlEncoder[TestEnum]
        .encodeValues(tdTemplate, TestEnum.Foo)
        .map(_.encode)
        .mkString("")
      assertTrue(
        result == "<td>Foo</td>"
      )
    },
    test("it should encode enum with value to <td>") {
      val result = HtmlEncoder[TestEnum]
        .encodeValues(tdTemplate, TestEnum.Bar("isHappy"))
        .map(_.encode)
        .mkString("")
      assertTrue(
        result == "<td>Bar(isHappy)</td>"
      )
    },
    test("it should encode enum name to <td>") {
      val result = HtmlEncoder[TestEnum].encodeParams(tdParamsTemplate).map(_.encode).mkString("")
      assertTrue(result == "<td>TestEnum</td>")
    },
    test("it should encode Int value parameter name as \"\"") {
      val foo = HtmlEncoder[Int].encodeParams(tdParamsTemplate)
      assertTrue(foo.map(_.encode).mkString("") == "<td></td>")
    },
    test("it should encode Int value to <td>") {
      val bar = 42
      val foo = HtmlEncoder[Int].encodeValues(tdTemplate, bar)
      assertTrue(foo.map(_.encode).mkString("") == "<td>42</td>")
    },
    test("it should encode enum name inside Option to <td>") {
      val result = HtmlEncoder[Option[TestEnum]].encodeParams(tdParamsTemplate).map(_.encode).mkString("")
      assertTrue(result == "<td>TestEnum</td>")
    },
    test("it should encode DataOpt parameters to <td>") {
      val result = HtmlEncoder[DataOpt].encodeParams(tdParamsTemplate).map(_.encode).mkString("")
      assertTrue(result == "<td>data</td>")
    },
    test("it should encode DataOpt value to <td>") {
      val someResult =
        HtmlEncoder[DataOpt].encodeValues(tdTemplate, DataOpt(Some("value"))).map(_.encode).mkString("")
      val noneResult = HtmlEncoder[DataOpt].encodeValues(tdTemplate, DataOpt(None)).map(_.encode).mkString("")
      assertTrue(someResult == "<td>value</td>")
      assertTrue(noneResult == "<td></td>")
    },
    test("it should encode annotated case class params to <td>") {
      val result = HtmlEncoder[AnnotatedData].encodeParams(annotationTemplate).map(_.encode).mkString("")
      assertTrue(result == "<td class=\"testAnnotation\">data</td><td class=\"value\">data1</td>")
    },
    test("it should encode annotated case class values to <td>") {
      val annotatedData = AnnotatedData("realValue", 42)
      val result = HtmlEncoder[AnnotatedData].encodeValues(annotationTemplate, annotatedData).map(_.encode).mkString("")
      assertTrue(result == "<td class=\"testAnnotation\">realValue</td><td class=\"value\">42</td>")
    },
    test("it should encode annotated case class with Option values to <td>") {
      val someData = AnnotatedOpt(Some("realValue"), Some(42))
      val noneAndSomeData = AnnotatedOpt(Some("realValue"), None)
      val someResult = HtmlEncoder[AnnotatedOpt].encodeValues(annotationTemplate, someData).map(_.encode).mkString("")
      val noneAndSomeResult =
        HtmlEncoder[AnnotatedOpt].encodeValues(annotationTemplate, noneAndSomeData).map(_.encode).mkString("")
      assertTrue(someResult == "<td class=\"testAnnotation\">realValue</td><td class=\"value\">42</td>")
      assertTrue(noneAndSomeResult == "<td class=\"testAnnotation\">realValue</td><td class=\"value\"></td>")
    },
    test("it should encode empty list of case classes into <td> as \"\"") {
      val result = HtmlEncoder[Seq[Data]].encodeValues(tdTemplate, List()).map(_.encode).mkString("")
      assertTrue(result == "<td></td>")
    },
    test("it should encode list of case classes into <td>") {
      val result = HtmlEncoder[Seq[Data]].encodeValues(tdTemplate, List(Data("value", 1), Data("value2", 2))).map(_.encode).mkString("")
      assertTrue(result == "<td>value</td><td>1</td><td>value2</td><td>2</td>")
    },
  )

  val errorMsgSuite = suite("ErrorMsg extension")(test("it should filter error messages based on param name") {
      val someErrors = Some(Seq(ErrorMsg("foo", "bar"), ErrorMsg("foo", "baz"), ErrorMsg("bar", "baz")))
      assertTrue(someErrors.errorMsgs("foo") == Some(Seq("bar", "baz")))
      assertTrue(someErrors.errorMsgs("expect_none") == None)
    })

  override def spec = htmlEncoderSuite + errorMsgSuite 
