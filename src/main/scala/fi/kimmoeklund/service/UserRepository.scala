package fi.kimmoeklund.service

import fi.kimmoeklund.domain.*
import zio.*

import java.util.UUID

trait UserRepository:
  def checkUserPassword(userName: String, password: String): IO[ErrorCode, User]
  def addUser(user: NewPasswordUser): IO[InsertDataError, User]
  def updateUser(user: User): IO[ExistingEntityError, User]
  def getUsers(userIdOpt: Option[UserId]): IO[ExistingEntityError, List[User]]
  def deleteUser(id: UserId): IO[ErrorCode, Unit]

