package fi.kimmoeklund.service

import fi.kimmoeklund.domain.*
import zio.*

import java.util.UUID

trait PageService

trait UserRepository: 
  def checkUserPassword(userName: String, password: String): IO[ErrorCode, User]

  def addUser(user: NewPasswordUser): IO[ErrorCode, User]

  def addRole(role: Role): IO[ErrorCode, Role]

  def addOrganization(org: Organization): IO[ErrorCode, Organization]

  def addPermission(permission: Permission): IO[ErrorCode, Permission]

  def updateUserRoles: IO[ErrorCode, Option[User]]

  def effectivePermissionsForUser(id: String): IO[ErrorCode, Option[Seq[Permission]]]

  def getUsers: IO[ErrorCode, List[User]]

  def getRoles: IO[ErrorCode, List[Role]]

  def getRolesByIds(ids: Seq[UUID]): IO[ErrorCode, List[Role]]

  def getOrganizations: IO[ErrorCode, List[Organization]]

  def getOrganizationById(id: UUID): IO[ErrorCode, Organization]

  def getPermissions: IO[ErrorCode, List[Permission]]

  def getPermissionsById(ids: Seq[UUID]): IO[ErrorCode, List[Permission]]

  def deletePermission(id: UUID): IO[ErrorCode, Unit]

  def deleteOrganization(id: UUID): IO[ErrorCode, Unit]

  def deleteRole(id: UUID): IO[ErrorCode, Unit]

  def deleteUser(id: UUID): IO[ErrorCode, Unit]
