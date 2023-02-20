package fi.kimmoeklund.domain

import java.util.UUID

case class PasswordCredentials(userId: UUID, userName: String, password: String)

