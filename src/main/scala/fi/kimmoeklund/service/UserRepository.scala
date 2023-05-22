package fi.kimmoeklund.service

import zio._
import fi.kimmoeklund.domain._

trait UserRepository:
  def checkUserPassword(userName: String, password: String): Task[Option[User]]

  def addUser(user: User, pwdCredentials: PasswordCredentials, organization: Organization): Task[Unit]

  def addRole(role: Role): Task[Unit]

  def addPermission(permission: Permission): Task[Unit]
  
  def updateUserRoles(): Task[Option[User]]
  
  def effectivePermissionsForUser(id: String): Task[Option[Seq[Permission]]]

  def getUsers: Task[List[User]]

object UserRepository:
  def checkUserPassword(userName: String, password: String): ZIO[UserRepository, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.checkUserPassword(userName, password))

  def addUser(user: User, pwdCredentials: PasswordCredentials, organization: Organization): ZIO[UserRepository, Throwable, Unit] = 
    ZIO.serviceWithZIO[UserRepository](_.addUser(user, pwdCredentials, organization))

  def addRole(role: Role): ZIO[UserRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.addRole(role))

  def addPermission(permission: Permission): ZIO[UserRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[UserRepository](_.addPermission(permission))

  def updateUserRoles(): ZIO[UserRepository, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.updateUserRoles())

  def effectivePermissionsForUser(id: String): ZIO[UserRepository, Throwable, Option[Seq[Permission]]] = 
    ZIO.serviceWithZIO[UserRepository](_.effectivePermissionsForUser(id))

  def getUsers(): ZIO[UserRepository, Throwable, List[User]] = 
    ZIO.serviceWithZIO[UserRepository](_.getUsers)

