package fi.kimmoeklund.service

import fi.kimmoeklund.domain.Permission
import fi.kimmoeklund.domain.ErrorCode
import java.util.UUID
import fi.kimmoeklund.domain.ExistingEntityError
import zio.IO
import io.getquill.jdbczio.Quill
import io.getquill.*
import fi.kimmoeklund.domain.*
import java.sql.SQLException
import io.github.arainko.ducktape.*
import zio.*

trait PermissionRepository:
  def addPermission(permission: Permission): IO[ErrorCode, Permission]
  def getPermissions: IO[ExistingEntityError, List[Permission]]
  def deletePermission(id: UUID): IO[ErrorCode, Unit]
  def getPermissionsByIds(ids: Seq[UUID]): IO[ExistingEntityError, List[Permission]]

final class PermissionRepositoryLive(quill: Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]])
    extends PermissionRepository:
  import quill.*

  override def addPermission(permission: Permission) =
    (for {
      _ <- run(
        query[Permissions].insertValue(
          lift(
            Permissions(
              permission.id,
              permission.target,
              permission.permission
            )
          )
        )
      )
    } yield (permission)).mapBoth(e => GeneralError.Exception(e.getMessage), p => p)

  override def getPermissions =
    val users = run {
      query[Permissions]
    }.fold(
      _ => List(),
      list => list.map(p => Permission(p.id, p.target, p.permission))
    )
    users

  override def getPermissionsByIds(ids: Seq[UUID]) = run {
    quote { query[Permissions].filter(p => liftQuery(ids).contains(p.id)) }
  }.map(l => l.map(p => p.to[Permission]))
    .filterOrElseWith(permissions => permissions.size == ids.size)(permissions =>
      ZIO.fail(ExistingEntityError.EntityNotFound(ids.diff(permissions.map(_.id)).mkString(",")))
    )
    .mapError({
      case e: SQLException => ExistingEntityError.Exception(e.getMessage)
      case e: ExistingEntityError => e
    })

  override def deletePermission(id: UUID): IO[ErrorCode, Unit] = {
    run {
      query[Permissions].filter(p => p.id == lift(id)).delete
    }.mapBoth(e => GeneralError.Exception(e.getMessage), _ => ())
  }

object PermissionRepositoryLive:
  def layer(keys: Seq[String]) =
    val repos = ZIO
      .foreach(keys) { key =>
        for {
          quill <- ZIO.serviceAt[Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]]](key)
        } yield (key, PermissionRepositoryLive(quill.get).asInstanceOf[PermissionRepository])
      }
      .map(t => ZEnvironment(t.toMap))
    ZLayer.fromZIOEnvironment(repos)
