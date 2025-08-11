package fi.kimmoeklund.repository

import io.getquill.jdbczio.Quill
import io.getquill.*
import fi.kimmoeklund.domain.*
import zio.* 
import java.util.UUID
import java.sql.SQLException
import MappedEncodings.given
import fi.kimmoeklund.html.pages.ValidRoleForm

type RoleRepository = Repository[Role, RoleId, ValidRoleForm]

final class RoleRepositoryLive extends RoleRepository:
  private def mapRoles(roles: Seq[(Roles, Option[PermissionGrants], Option[Permissions])]) =
    val permissionsMap = roles.groupMap(_._1)(_._3)
    roles
      .map(r =>
        Role(
          r._1.id,
          r._1.name,
          Set.from(permissionsMap(r._1).flatten.map(p => Permission(p.id, p.target, p.permission)))
        )
      )
      .distinct

  override def delete(using quill: QuillCtx)(id: RoleId): IO[ErrorCode, Unit] =
    import quill.*
    run {
      query[Roles].filter(r => r.id == lift(id)).delete
    }.mapBoth(e => GeneralError.Exception(e.getMessage), _ => ())

  override def getList(using quill: QuillCtx): IO[ExistingEntityError, Seq[Role]] = {
    import quill.*
    run {
      for {
        roles <- query[Roles]
        pg <- query[PermissionGrants].leftJoin(pg => roles.id == pg.roleId)
        p <- query[Permissions].leftJoin(p => p.id == pg.orNull.permissionId)
      } yield (roles, pg, p)
    }.mapBoth(
      e => ExistingEntityError.Exception(e.getMessage),
      list => this.mapRoles(list)
    )
  }

  override def getByIds(using quill: QuillCtx)(ids: Set[RoleId]): IO[ExistingEntityError, Set[Role]] =
    import quill.*
    val uuids = ids.map(RoleId.unwrap)
    run {
      for {
        roles <- query[Roles].filter(r => liftQuery(uuids).contains(r.id))
        pg <- query[PermissionGrants].leftJoin(pg => roles.id == pg.roleId)
        p <- query[Permissions].leftJoin(p => p.id == pg.orNull.permissionId)
      } yield (roles, pg, p)
    }.map(list => this.mapRoles(list).toSet)
      .filterOrElseWith(roles => roles.size == ids.size)(roles =>
          ZIO.fail(ExistingEntityError.EntityNotFound(ids.diff(roles.map(_.id).toSet).mkString(",")))
      )
      .mapError({
        case e: SQLException => ExistingEntityError.Exception(e.getMessage)
        case e: ExistingEntityError => e
      })

  override def add(using quill: QuillCtx)(validForm: ValidRoleForm): IO[ErrorCode, Role] =
    import quill.*
    val roleId = RoleId(UUID.randomUUID())
    val newRole = Roles(roleId, validForm.name)
    transaction {
      for {
        _ <- run(query[Roles].insertValue(lift(newRole)))
        _ <- run {
            liftQuery(validForm.permissions).foreach(p =>
              query[PermissionGrants].insertValue(
                PermissionGrants(lift(newRole.id), p)
              )
            )
          }
        permissions <- run {
            query[Permissions].filter(p => liftQuery(validForm.permissions.map(PermissionId.unwrap)).contains(p.id))
          }.map(_.map(p => Permission(p.id, p.target, p.permission)).toSet)
      } yield Role(roleId, validForm.name, permissions)
    }.mapBoth(
      e => GeneralError.Exception(e.getMessage),
      role => role
    )

  override def update(using quill: QuillCtx)(validForm: ValidRoleForm) = ???

