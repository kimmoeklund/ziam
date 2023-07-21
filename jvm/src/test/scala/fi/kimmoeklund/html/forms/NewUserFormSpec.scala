package fi.kimmoeklund.html.forms

import fi.kimmoeklund.domain.FormError.*
import zio.http.FormField
import zio.test.*

object NewUserFormSpec extends ZIOSpecDefault {
  override def spec = suite("NewUserForm")(
    test("it should fail for missing values") {
      NewUserForm
        .fromOptions(None, None, None, None, None, None)
        .fold(
          e => {
            assertTrue(e.contains(MissingInput("username")))
            assertTrue(e.contains(MissingInput("password")))
            assertTrue(e.contains(MissingInput("password_confirmation")))
            assertTrue(e.contains(MissingInput("organization")))
          },
          _ => assertTrue(false)
        )
    },
    test("it should fail for password mismatch") {
      NewUserForm
        .fromOptions(
          None,
          None,
          None,
          None,
          Some(FormField.Simple("password", "password")),
          Some(FormField.Simple("password_confirmation", "different"))
        )
        .fold(
          e => {
            assertTrue(e.contains(PasswordsDoNotMatch))
          },
          _ => assertTrue(false)
        )
    }
  )
}
