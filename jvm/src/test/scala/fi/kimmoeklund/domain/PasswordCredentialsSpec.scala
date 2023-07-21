package fi.kimmoeklund.domain

import fi.kimmoeklund.domain.PasswordError.{PasswordConfirmationMissing, PasswordMissing, UserNameMissing}
import zio.test.*

import java.util.UUID

object PasswordCredentialsSpec extends ZIOSpecDefault {
  override def spec = suite("NewPasswordCredentials")(
    test("it should fail for missing values") {
      NewPasswordCredentials
        .fromOptions(None, None, None)
        .fold(
          e => {
            assertTrue(e.contains(UserNameMissing))
            assertTrue(e.contains(PasswordMissing))
            assertTrue(e.contains(PasswordConfirmationMissing))
          },
          _ => assertTrue(false)
        )
    },
    test("it should fail if passwords do not match") {
      NewPasswordCredentials
        .fromOptions(Some("username"), Some("password"), Some("password2"))
        .fold(
          e => {
            assertTrue(e.contains(PasswordError.PasswordsDoNotMatch))
          },
          _ => assertTrue(false)
        )
    },
    test("it should succeed when all values are valid") {
      val uuid = UUID.randomUUID()
      NewPasswordCredentials
        .fromOptions(Some("username"), Some("password"), Some("password"))
        .fold(
          _ => assertTrue(false),
          c => {
            assertTrue(c.userName == "username")
            assertTrue(c.password == "password")
          }
        )
    }
  )
}
