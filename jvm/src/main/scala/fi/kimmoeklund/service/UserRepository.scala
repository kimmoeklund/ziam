package fi.kimmoeklund.service

import fi.kimmoeklund.domain.{NewPasswordUser, Organization, PasswordCredentials, Permission, Role, User, Errors}
import zio.*

import java.util.UUID
import fi.kimmoeklund.domain.ErrorCode

trait UserRepository:
  def checkUserPassword(userName: String, password: String): IO[ErrorCode, Option[User]]

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

object UserRepository:
  def checkUserPassword(userName: String, password: String): ZIO[UserRepository, ErrorCode, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.checkUserPassword(userName, password))

  def addOrganization(org: Organization): ZIO[UserRepository, ErrorCode, Organization] =
    ZIO.serviceWithZIO[UserRepository](_.addOrganization(org))

  def addUser(user: NewPasswordUser): ZIO[UserRepository, ErrorCode, User] =
    ZIO.serviceWithZIO[UserRepository](_.addUser(user))

  def addRole(role: Role): ZIO[UserRepository, ErrorCode, Role] =
    ZIO.serviceWithZIO[UserRepository](_.addRole(role))

  def addPermission(permission: Permission): ZIO[UserRepository, ErrorCode, Permission] =
    ZIO.serviceWithZIO[UserRepository](_.addPermission(permission))

  def updateUserRoles(): ZIO[UserRepository, ErrorCode, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.updateUserRoles)

  def effectivePermissionsForUser(id: String): ZIO[UserRepository, ErrorCode, Option[Seq[Permission]]] =
    ZIO.serviceWithZIO[UserRepository](_.effectivePermissionsForUser(id))

  def getUsers(): ZIO[UserRepository, ErrorCode, List[User]] =
    ZIO.serviceWithZIO[UserRepository](_.getUsers)

  def getRoles(): ZIO[UserRepository, ErrorCode, List[Role]] =
    ZIO.serviceWithZIO[UserRepository](_.getRoles)

  def getRolesByIds(ids: Seq[UUID]): ZIO[UserRepository, ErrorCode, List[Role]] =
    ZIO.serviceWithZIO[UserRepository](_.getRolesByIds(ids))

  def getPermissions(): ZIO[UserRepository, ErrorCode, List[Permission]] =
    ZIO.serviceWithZIO[UserRepository](_.getPermissions)

  def getOrganizations(): ZIO[UserRepository, ErrorCode, List[Organization]] =
    ZIO.serviceWithZIO[UserRepository](_.getOrganizations)

  def getOrganizationById(id: UUID): ZIO[UserRepository, ErrorCode, Organization] =
    ZIO.serviceWithZIO[UserRepository](_.getOrganizationById(id))

  def getPermissionsById(ids: Seq[UUID]): ZIO[UserRepository, ErrorCode, List[Permission]] =
    ZIO.serviceWithZIO[UserRepository](_.getPermissionsById(ids))

  def deletePermission(id: UUID): ZIO[UserRepository, ErrorCode, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.deletePermission(id))

  def deleteOrganization(id: UUID): ZIO[UserRepository, ErrorCode, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.deleteOrganization(id))

  def deleteRole(id: UUID): ZIO[UserRepository, ErrorCode, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.deleteRole(id))

  def deleteUser(id: UUID): ZIO[UserRepository, ErrorCode, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.deleteUser(id))
