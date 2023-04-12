package fi.kimmoeklund.service

import zio._
import fi.kimmoeklund.domain._

trait UserRepository:
  def checkUserPassword(userName: String, password: String): Task[Option[User]]

  def add(user: User, pwdCredentials: PasswordCredentials, organization: Organization): Task[Unit]
  
  def updateUserRoles(): Task[Option[User]]
  
  def updateUserGroups(): Task[Option[User]]
  
  def effectivePermissionsForUser(id: String): Task[Option[Seq[Permission]]]

object UserRepository:
  def checkUserPassword(userName: String, password: String): ZIO[UserRepository, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.checkUserPassword(userName, password))

  def add(user: User, pwdCredentials: PasswordCredentials, organization: Organization): ZIO[UserRepository, Throwable, Unit] = 
    ZIO.serviceWithZIO[UserRepository](_.add(user, pwdCredentials, organization))

  def updateUserRoles(): ZIO[UserRepository, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.updateUserRoles())

  def updateUserGroups(): ZIO[UserRepository, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.updateUserGroups())

  def effectivePermissionsForUser(id: String): ZIO[UserRepository, Throwable, Option[Seq[Permission]]] = 
    ZIO.serviceWithZIO[UserRepository](_.effectivePermissionsForUser(id))

