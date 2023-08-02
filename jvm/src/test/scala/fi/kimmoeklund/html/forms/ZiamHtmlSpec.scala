package fi.kimmoeklund.html.forms

import zio.test.*
import zio.http.html.*
import fi.kimmoeklund.domain.User
import java.util.UUID
import fi.kimmoeklund.domain.Organization
import fi.kimmoeklund.html.wrapWith

object ZiamHtmlSpec extends ZIOSpecDefault:
  override def spec = suite("ZiamHtml")(test("it should wrap User parameters to <td>") {
    val user = new User(UUID.randomUUID(), "test", Organization(UUID.randomUUID(), "test-org"), Seq(), Seq())
    assertTrue(
      user
        .wrapWith(td)
        .map(_.encode)
        .mkString(
          ""
        ) == s"<td>${user.id.toString}</td><td>${user.name}</td><td>${user.organization.id.toString}</td><td>${user.organization.name}</td>"
    )
  })
