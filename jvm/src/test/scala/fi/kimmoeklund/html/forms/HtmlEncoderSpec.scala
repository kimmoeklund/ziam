package fi.kimmoeklund.html.forms

import zio.test.*
import zio.http.html.*
import fi.kimmoeklund.domain.User
import java.util.UUID
import fi.kimmoeklund.domain.Organization
//import fi.kimmoeklund.html.encodeValues
import fi.kimmoeklund.html.HtmlEncoder
import fi.kimmoeklund.domain.LoginType
import fi.kimmoeklund.html.pages.UserView
import scala.annotation.Annotation

enum TestEnum {
  case Foo
  case Bar(happy: String)
}

given HtmlEncoder[TestEnum] = HtmlEncoder.derived[TestEnum]
given HtmlEncoder[LoginType] = HtmlEncoder.derived[LoginType]

val tdTemplate = (value: String) => td(value)
val annotationMapper = (a: Annotation) => Html.fromUnit(())

object HtmlEncoderSpec extends ZIOSpecDefault:
  override def spec = suite("HtmlEncoder")(
    test("it should wrap User parameters values to <td>") {
      val user = UserView(UUID.randomUUID(), "name", "test-org", List(), List())
      assertTrue(
        HtmlEncoder[UserView]
          .encodeValues(tdTemplate, user)
          .map(_.encode)
          .mkString(
            ""
          ) == s"<td>${user.id.toString}</td><td>${user.name}</td><td>${user.organization}</td>"
      )
    },
    test("it should wrap User parameter names with <td>") {
      val result = HtmlEncoder[UserView]
        .encodeParams(tdTemplate, annotationMapper)
        .map(_.encode)
        .mkString("")
      assertTrue(
        result == "<td>id</td><td>name</td><td>organization</td><td>roles</td><td>logins</td>"
      )
    },
    test("it should wrap enum value inside <td>") {
      val result = HtmlEncoder[TestEnum]
        .encodeValues(tdTemplate, TestEnum.Foo)
        .map(_.encode)
        .mkString("")
      assertTrue(
        result == "<td>Foo</td>"
      )
    },
    test("it should wrap enum value inside <td>") {
      val result = HtmlEncoder[TestEnum]
        .encodeValues(tdTemplate, TestEnum.Bar("isHappy"))
        .map(_.encode)
        .mkString("")
      assertTrue(
        result == "<td>Bar(isHappy)</td>"
      )
    },
    test("it should wrap enum name inside <td>") {
      val result = HtmlEncoder[TestEnum].encodeParams(tdTemplate, annotationMapper).map(_.encode).mkString("")
      assertTrue(result == "<td>TestEnum</td>")
    },
    test("it should label Int as \"\"") {
      val foo = HtmlEncoder[Int].encodeParams(tdTemplate, annotationMapper)
      assertTrue(foo.map(_.encode).mkString("") == "<td></td>")
    })
