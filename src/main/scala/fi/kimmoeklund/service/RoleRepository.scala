package fi.kimmoeklund.service

import java.util.UUID
import zio.*
import fi.kimmoeklund.domain.*

trait RoleRepository:
  def deleteRole(id: UUID): IO[ErrorCode, Unit]
  def updateUserRoles: IO[ErrorCode, Option[User]]
  def getRoles: IO[GetDataError, Seq[Role]]
  def getRolesByIds(ids: Seq[RoleId]): IO[GetDataError, Seq[Role]]
  def addRole(role: Role): IO[ErrorCode, Role]
