package fi.kimmoeklund.service

import fi.kimmoeklund.domain.{PasswordCredentials, Permission, Role, User}
import zio.*

import java.util.UUID

trait UserRepository:
  def checkUserPassword(userName: String, password: String): Task[Option[User]]

  def addUser(user: User, pwdCredentials: PasswordCredentials): Task[Unit]

  def addRole(role: Role): Task[Unit]

  def addPermission(permission: Permission): Task[Permission]
  
  def updateUserRoles(): Task[Option[User]]
  
  def effectivePermissionsForUser(id: String): Task[Option[Seq[Permission]]]

  def getUsers: Task[List[User]]

  def getPermissions: Task[List[Permission]]

  def deletePermission(id: UUID): Task[Unit]

object UserRepository:
  def checkUserPassword(userName: String, password: String): ZIO[UserRepository, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.checkUserPassword(userName, password))

  def addUser(user: User, pwdCredentials: PasswordCredentials): ZIO[UserRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.addUser(user, pwdCredentials))

  def addRole(role: Role): ZIO[UserRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.addRole(role))

  def addPermission(permission: Permission): ZIO[UserRepository, Throwable, Permission] =
    ZIO.serviceWithZIO[UserRepository](_.addPermission(permission))

  def updateUserRoles(): ZIO[UserRepository, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.updateUserRoles())

  def effectivePermissionsForUser(id: String): ZIO[UserRepository, Throwable, Option[Seq[Permission]]] = 
    ZIO.serviceWithZIO[UserRepository](_.effectivePermissionsForUser(id))

  def getUsers(): ZIO[UserRepository, Throwable, List[User]] = 
    ZIO.serviceWithZIO[UserRepository](_.getUsers)

  def getPermissions(): ZIO[UserRepository, Throwable, List[Permission]] =
    ZIO.serviceWithZIO[UserRepository](_.getPermissions)

  def deletePermission(id: UUID): ZIO[UserRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.deletePermission(id))