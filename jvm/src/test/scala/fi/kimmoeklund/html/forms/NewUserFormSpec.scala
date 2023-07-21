package fi.kimmoeklund.html.forms

import zio.test.*
import fi.kimmoeklund.domain.PasswordError.{PasswordConfirmationMissing, PasswordMissing, UserNameMissing, PasswordsDoNotMatch}
import fi.kimmoeklund.html.forms.NewUserFormErrors.*
import zio.http.FormField

object NewUserFormSpec extends ZIOSpecDefault {
  override def spec = suite("NewUserForm")(
    test("it should fail for missing values") {
      NewUserForm.fromOptions(None, None, None, None, None, None)
      .fold(
        e => {
            assertTrue(e.contains(UserNameMissing))
            assertTrue(e.contains(PasswordMissing))
            assertTrue(e.contains(PasswordConfirmationMissing))
            assertTrue(e.contains(OrganizationIdMissing))
        },
        _ => assertTrue(false))
    },
    test("it should fail for password mismatch") {
      NewUserForm.fromOptions(None, None, None, None, Some(FormField.Simple("password", "password")), 
        Some(FormField.Simple("password_confirmation", "different")))
      .fold(
        e => {
            assertTrue(e.contains(PasswordsDoNotMatch))
        },
        _ => assertTrue(false))
    }
  )
}
