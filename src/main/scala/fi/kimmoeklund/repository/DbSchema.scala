package fi.kimmoeklund.repository

import java.util.UUID
import fi.kimmoeklund.domain.*
import io.getquill.MappedEncoding

case class Members(id: UserId, name: String)
case class Roles(id: RoleId, name: String)
case class RoleGrants(roleId: RoleId, memberId: UserId)
case class PermissionGrants(roleId: RoleId, permissionId: PermissionId)
case class Permissions(id: PermissionId, target: String, permission: Int)
case class PasswordCredentials(
    memberId: UserId,
    userName: String,
    passwordHash: String
)

object MappedEncodings:
  given MappedEncoding[PermissionId, UUID](PermissionId.unwrap)
  given MappedEncoding[UUID, PermissionId](PermissionId.apply)
  given MappedEncoding[RoleId, UUID](RoleId.unwrap)
  given MappedEncoding[UUID, RoleId](RoleId.apply)
  given MappedEncoding[UserId, UUID](UserId.unwrap)
  given MappedEncoding[UUID, UserId](UserId.apply)

