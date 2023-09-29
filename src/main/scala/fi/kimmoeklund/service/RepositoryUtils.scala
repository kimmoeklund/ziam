package fi.kimmoeklund.service

import java.util.UUID
import fi.kimmoeklund.domain.*
import io.github.arainko.ducktape.*

case class Members(id: UUID, name: String)
case class Roles(id: UUID, name: String)
case class RoleGrants(roleId: UUID, memberId: UUID)
case class PermissionGrants(roleId: UUID, permissionId: UUID)
case class Permissions(id: UUID, target: String, permission: Int)
case class PasswordCredentials(
    memberId: UUID,
    userName: String,
    passwordHash: String
)

trait RepositoryUtils:
  protected def toRoles(role: Role): Roles = Roles(RoleId.unwrap(role.id), role.name)


