package fi.kimmoeklund.html.forms

import zio.test.*
import zio.http.html.*
import fi.kimmoeklund.domain.User
import java.util.UUID
import fi.kimmoeklund.domain.Organization
import fi.kimmoeklund.html.wrapWith
import fi.kimmoeklund.html.HtmlEncoder
import fi.kimmoeklund.domain.LoginType

enum TestEnum {
  case Foo
  case Bar
}

object HtmlEncoderSpec extends ZIOSpecDefault:
  override def spec = suite("HtmlEncoder")(
    test("it should wrap User parameters values to <td>") {
      val user = User(UUID.randomUUID(), "test", Organization(UUID.randomUUID(), "test-org"), Seq(), Seq())
      assertTrue(
        user
          .wrapWith(td)
          .map(_.encode)
          .mkString(
            ""
          ) == s"<td>${user.id.toString}</td><td>${user.name}</td><td>${user.organization.id.toString}</td><td>${user.organization.name}</td>"
      )
    },
    test("it should wrap User parameter names with <td>") {
      assertTrue(HtmlEncoder[User].wrapParametersWith(td).map(_.encode).mkString("") == "<td>id</td><td>name</td><td>organization</td><td>roles</td><td>logins</td>")
    },
    test("it should wrap User parameter names capitalized with <td> ") {
      val result = HtmlEncoder[User].wrapParametersWith(td, _.capitalize).map(_.encode).mkString("") 
      assertTrue(result == "<td>Id</td><td>Name</td><td>Organization</td><td>Roles</td><td>Logins</td>")
    },
    test("it should wrap enum value inside <td>") {
      assertTrue(LoginType.PasswordCredentials.wrapWith(td).map(_.encode).mkString("") == "<td>PasswordCredentials</td>")
    },
    test("it should wrap enum name inside <td>") {
      val foo = HtmlEncoder[TestEnum].wrapParametersWith(td).map(_.encode).mkString("")
      assertTrue(foo == "<td>TestEnum</td>")
    },
    test("it should wrap enum value inside <td>") {
      val foo = TestEnum.Foo.wrapWith(td).map(_.encode).mkString("")
      assertTrue(foo == "<td>Foo</td>")
    },
    test("it should label UUID as uuid") {
      val foo = HtmlEncoder[java.util.UUID].wrapParametersWith(td)
      assertTrue(foo.map(_.encode).mkString("") == "<td>uuid</td>")
    },
    test("it should label Int as int") {
      val foo = HtmlEncoder[Int].wrapParametersWith(td)
      assertTrue(foo.map(_.encode).mkString("") == "<td>int</td>")
    },
    test("it should label String as string") {
      val foo = HtmlEncoder[String].wrapParametersWith(td)
      assertTrue(foo.map(_.encode).mkString("") == "<td>string</td>")
    }
  )
