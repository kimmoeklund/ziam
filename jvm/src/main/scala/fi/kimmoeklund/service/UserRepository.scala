package fi.kimmoeklund.service

import fi.kimmoeklund.domain.{NewPasswordUser, Organization, PasswordCredentials, Permission, Role, User, Errors}
import zio.*

import java.util.UUID

trait UserRepository:
  def checkUserPassword(userName: String, password: String): Task[Option[User]]

  def addUser(user: NewPasswordUser): Task[User]

  def addRole(role: Role): Task[Role]

  def addOrganization(org: Organization): Task[Organization]

  def addPermission(permission: Permission): Task[Permission]
  
  def updateUserRoles: Task[Option[User]]
  
  def effectivePermissionsForUser(id: String): Task[Option[Seq[Permission]]]

  def getUsers: Task[List[User]]

  def getRoles: Task[List[Role]]

  def getRolesByIds(ids: Seq[UUID]): Task[List[Role]]

  def getOrganizations: Task[List[Organization]]

  def getOrganizationById(id: UUID): Task[Option[Organization]]

  def getPermissions: Task[List[Permission]]

  def getPermissionsById(ids: Seq[UUID]): Task[List[Permission]]

  def deletePermission(id: UUID): Task[Unit]

  def deleteOrganization(id: UUID): Task[Unit]

  def deleteRole(id: UUID): Task[Unit]

  def deleteUser(id: UUID): Task[Unit]

object UserRepository:
  def checkUserPassword(userName: String, password: String): ZIO[UserRepository, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.checkUserPassword(userName, password))

  def addOrganization(org: Organization): ZIO[UserRepository, Throwable, Organization] =
    ZIO.serviceWithZIO[UserRepository](_.addOrganization(org))

  def addUser(user: NewPasswordUser): ZIO[UserRepository, Throwable, User] =
    ZIO.serviceWithZIO[UserRepository](_.addUser(user))

  def addRole(role: Role): ZIO[UserRepository, Throwable, Role] =
    ZIO.serviceWithZIO[UserRepository](_.addRole(role))

  def addPermission(permission: Permission): ZIO[UserRepository, Throwable, Permission] =
    ZIO.serviceWithZIO[UserRepository](_.addPermission(permission))

  def updateUserRoles(): ZIO[UserRepository, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.updateUserRoles)

  def effectivePermissionsForUser(id: String): ZIO[UserRepository, Throwable, Option[Seq[Permission]]] = 
    ZIO.serviceWithZIO[UserRepository](_.effectivePermissionsForUser(id))

  def getUsers(): ZIO[UserRepository, Throwable, List[User]] = 
    ZIO.serviceWithZIO[UserRepository](_.getUsers)

  def getRoles(): ZIO[UserRepository, Throwable, List[Role]] =
    ZIO.serviceWithZIO[UserRepository](_.getRoles)

  def getRolesByIds(ids: Seq[UUID]): ZIO[UserRepository, Throwable, List[Role]] =
    ZIO.serviceWithZIO[UserRepository](_.getRolesByIds(ids))

  def getPermissions(): ZIO[UserRepository, Throwable, List[Permission]] =
    ZIO.serviceWithZIO[UserRepository](_.getPermissions)

  def getOrganizations(): ZIO[UserRepository, Throwable, List[Organization]] =
    ZIO.serviceWithZIO[UserRepository](_.getOrganizations)

  def getOrganizationById(id: UUID): ZIO[UserRepository, Throwable, Option[Organization]] =
    ZIO.serviceWithZIO[UserRepository](_.getOrganizationById(id))

  def getPermissionsById(ids: Seq[UUID]): ZIO[UserRepository, Throwable, List[Permission]] =
    ZIO.serviceWithZIO[UserRepository](_.getPermissionsById(ids))

  def deletePermission(id: UUID): ZIO[UserRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.deletePermission(id))

  def deleteOrganization(id: UUID): ZIO[UserRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.deleteOrganization(id))

  def deleteRole(id: UUID): ZIO[UserRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.deleteRole(id))

  def deleteUser(id: UUID): ZIO[UserRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.deleteUser(id))