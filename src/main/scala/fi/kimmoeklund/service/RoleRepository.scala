package fi.kimmoeklund.service

import io.getquill.jdbczio.Quill
import io.getquill.*
import fi.kimmoeklund.domain.*
import zio.* 
import java.util.UUID
import java.sql.SQLException

trait RoleRepository:
  def deleteRole(id: UUID): IO[ErrorCode, Unit]
  def updateUserRoles: IO[ErrorCode, Option[User]]
  def getRoles: IO[ExistingEntityError, Seq[Role]]
  def getRolesByIds(ids: Set[RoleId]): IO[ExistingEntityError, Set[Role]]
  def addRole(role: Role): IO[ErrorCode, Role]

final class RoleRepositoryLive(
    quill: Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]]) extends RoleRepository with RepositoryUtils:
  import quill.*
  
  private def mapRoles(roles: Seq[(Roles, Option[PermissionGrants], Option[Permissions])]) =
    val permissionsMap = roles.groupMap(_._1)(_._3)
    roles
      .map(r =>
        Role(
          RoleId(r._1.id),
          r._1.name,
          permissionsMap(r._1).flatten.map(p => Permission(p.id, p.target, p.permission))
        )
      )
      .distinct

  override def deleteRole(id: UUID): IO[ErrorCode, Unit] =
    run {
      query[Roles].filter(r => r.id == lift(id)).delete
    }.mapBoth(e => GeneralError.Exception(e.getMessage), _ => ())

  override def getRoles: IO[ExistingEntityError, Seq[Role]] = {
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

  override def getRolesByIds(ids: Set[RoleId]): IO[ExistingEntityError, Set[Role]] =
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

  override def addRole(role: Role): IO[ErrorCode, Role] =
    val newRole = Roles(RoleId.unwrap(role.id), role.name)
    transaction {
      for {
        _ <-
          run(query[Roles].insertValue(lift(newRole)))
        _ <-
          run {
            liftQuery(role.permissions).foreach(p =>
              query[PermissionGrants].insertValue(
                PermissionGrants(lift(RoleId.unwrap(role.id)), p.id)
              )
            )
          }
      } yield (newRole)
    }.mapBoth(
      e => GeneralError.Exception(e.getMessage),
      r => role
    )

  override def updateUserRoles: IO[ErrorCode, Option[User]] = ???

object RoleRepositoryLive:
  def sqliteLayer(keys: Seq[String]): ZLayer[
    Map[String, Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]]],
    Nothing,
    Map[String, RoleRepository]
  ] = {
    val repos = ZIO
      .foreach(keys) { key =>
        for {
          quill <- ZIO.serviceAt[Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]]](key)
        } yield (key, RoleRepositoryLive(quill.get).asInstanceOf[RoleRepository])
      }
      .map(t => ZEnvironment(t.toMap))
    ZLayer.fromZIOEnvironment(repos)
  }

