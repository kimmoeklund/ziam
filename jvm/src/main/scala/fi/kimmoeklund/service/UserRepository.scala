package fi.kimmoeklund.service

import fi.kimmoeklund.domain.*
import zio.*

import java.util.UUID

trait UserRepository:
  def checkUserPassword(userName: String, password: String): IO[ErrorCode, User]

  def addUser(user: NewPasswordUser): IO[InsertDataError, User]

  def addOrganization(org: Organization): IO[ErrorCode, Organization]

  def effectivePermissionsForUser(id: String): IO[ErrorCode, Option[Seq[Permission]]]

  def getUsers: IO[GetDataError, List[User]]

  def getOrganizations: IO[GetDataError, List[Organization]]

  def getOrganizationById(id: UUID): IO[GetDataError, Organization]

  def deleteOrganization(id: UUID): IO[ErrorCode, Unit]

  def deleteUser(id: UUID): IO[ErrorCode, Unit]


