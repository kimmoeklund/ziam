package fi.kimmoeklund.html.forms

import fi.kimmoeklund.domain.FormError.*
import zio.http.FormField
import zio.test.*

object NewUserFormSpec extends ZIOSpecDefault {
  override def spec = suite("NewUserForm")(
    test("it should fail for missing values") {
      ValidUserForm
        .from(UserForm(None, None, None, None, None, None))
        .fold(
          e => {
            assertTrue(e.contains(Missing("username")))
            assertTrue(e.contains(Missing("password")))
            assertTrue(e.contains(Missing("password_confirmation")))
            assertTrue(e.contains(Missing("organization")))
          },
          _ => assertTrue(false)
        )
    },
    test("it should fail for password") {
      ValidUserForm
        .from(
          UserForm(
            None,
            None,
            Some("password"),
            Some("password_confirmation"),
            None,
            None
          )
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
