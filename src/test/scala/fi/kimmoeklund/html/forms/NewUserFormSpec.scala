package fi.kimmoeklund.html.forms

import fi.kimmoeklund.domain.FormError.*
import zio.http.FormField
import zio.test.*

object NewUserFormSpec extends ZIOSpecDefault {
  override def spec = suite("FormValidators")(
    test("it should fail for missing values") {
      FormValidators
        .newUser(UserForm(None, None, None, None, None, None))
        .fold(
          e => {
            assertTrue(e.contains(Missing("name")))
            assertTrue(e.contains(Missing("username")))
            assertTrue(e.contains(Missing("password")))
            assertTrue(e.contains(Missing("password_confirmation")))
          },
          _ => assertTrue(false)
        )
    },
    test("it should fail for password") {
      FormValidators
        .newUser(
          UserForm(
            None,
            None,
            None,
            Some("password"),
            Some("password_confirmation"),
            None,
          )
        )
        .fold(
          e => {
            assertTrue(e.contains(PasswordsDoNotMatch))
            assertTrue(e.contains(Missing("name")))
            assertTrue(e.contains(Missing("username")))
          },
          _ => assertTrue(false)
        )
    },
    test("it should not accept empty username") {
      FormValidators
        .newUser(
          UserForm(
            None,
            None,
            Some(""),
            Some("password"),
            Some("password"),
            None
          )
        )
        .fold(
          e => {
            assertTrue(e.contains(Missing("username")))
          },
          _ => assertTrue(false)
        )
    },
  )
}
