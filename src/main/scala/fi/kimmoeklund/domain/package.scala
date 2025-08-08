package fi.kimmoeklund.domain

trait Identifiable:
  val id: PermissionId | RoleId | UserId | BlogId
