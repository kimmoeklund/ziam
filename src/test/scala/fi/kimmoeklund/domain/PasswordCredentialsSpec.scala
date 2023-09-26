package fi.kimmoeklund.domain

import fi.kimmoeklund.domain.FormError.*
import zio.test.*

import java.util.UUID

object PasswordCredentialsSpec extends ZIOSpecDefault {
  override def spec = suite("NewPasswordCredentials")(
    test("it should fail for missing values") {
      NewPasswordCredentials
        .fromOptions(None, None, None)
        .fold(
          e => {
            assertTrue(e.contains(Missing("username")))
            assertTrue(e.contains(Missing("password")))
            assertTrue(e.contains(Missing("password_confirmation")))
          },
          _ => assertTrue(false)
        )
    },
    test("it should fail if passwords do not match") {
      NewPasswordCredentials
        .fromOptions(Some("username"), Some("password"), Some("password2"))
        .fold(
          e => {
            assertTrue(e.contains(PasswordsDoNotMatch))
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
