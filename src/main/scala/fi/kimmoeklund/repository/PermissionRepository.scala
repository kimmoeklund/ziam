package fi.kimmoeklund.repository

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
import MappedEncodings.given

type PermissionRepository = Repository[Permission, PermissionId]

final class PermissionRepositoryLive extends PermissionRepository:

  override def add(using quill: QuillCtx)(permission: Permission) =
    import quill.*
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

  override def getList(using quill: QuillCtx) =
    import quill.*
    val users = run {
      query[Permissions]
    }.fold(
      _ => List(),
      list => list.map(p => Permission(p.id, p.target, p.permission))
    )
    users

  override def getByIds(using quill: QuillCtx)(ids: Set[PermissionId]) = 
    import quill.*
    val uuids = ids.map(PermissionId.unwrap(_))
    run {
      quote { query[Permissions].filter(p => liftQuery(uuids).contains(p.id)) }
    }.map(l => l.map(p => p.to[Permission]).toSet) 
      .filterOrElseWith(permissions => permissions.size == ids.size)(permissions =>
          ZIO.fail(ExistingEntityError.EntityNotFound(ids.diff(permissions.map(_.id).toSet).mkString(",")))
      )
      .mapError({
        case e: SQLException        => ExistingEntityError.Exception(e.getMessage)
        case e: ExistingEntityError => e
      })

  override def delete(using quill: QuillCtx)(id: PermissionId): IO[ErrorCode, Unit] = 
    import quill.*
    run {
      query[Permissions].filter(p => p.id == lift(id)).delete
    }.mapBoth(e => GeneralError.Exception(e.getMessage), _ => ())

  override def update(using quill: QuillCtx)(permission: Permission) = ???

