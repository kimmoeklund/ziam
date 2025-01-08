package fi.kimmoeklund.repository

import com.outr.scalapass.Argon2PasswordFactory
import fi.kimmoeklund.domain.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import io.github.arainko.ducktape.*
import zio.*
import MappedEncodings.given 

import java.sql.SQLException
import java.util.UUID

type UsersJoin = List[(Members, Option[Roles], Option[Permissions], Option[PasswordCredentials])]

final class UserRepositoryLive(argon2Factory: Argon2PasswordFactory):

  private def mapUsers(
      users: UsersJoin
  ) =
    val members     = users.map(_._1).distinct
    val permissions = users.groupMap(_._2.orNull)(_._3)
    val roles       = users.groupMap(_._1)(_._2)
    val creds       = users.groupMap(_._1)(_._4)
    members.map(m =>
      User(
        m.id,
        m.name,
        roles(m).flatten.distinct
          .map(r =>
            Role(
              r.id,
              r.name,
              Set.from(permissions(r).flatten.map(p => Permission(p.id, p.target, p.permission)))
            )
          )
          .toSet,
        Set.from(creds(m).flatten.map(c => Login(c.userName, LoginType.PasswordCredentials)))
      )
    )

  def checkUserPassword(using quill: QuillCtx)(
      userName: String,
      password: String
  ): IO[ErrorCode, User] = {
    import quill.*
    run {
      for {
        creds <- query[PasswordCredentials].filter(p => p.userName == lift(userName))
        m     <- query[Members].join(m => m.id == creds.memberId)
        rg    <- query[RoleGrants].leftJoin(rg => rg.memberId == m.id)
        r     <- query[Roles].leftJoin(r => r.id == rg.orNull.roleId)
        pg    <- query[PermissionGrants].leftJoin(pg => pg.roleId == r.orNull.id)
        p     <- query[Permissions].leftJoin(p => p.id == pg.orNull.permissionId)
      } yield (m, r, p, creds)
    }.flatMap(resultsRaw =>
      ZIO.blocking(
        resultsRaw.headOption match {
          case Some(row) =>
            if argon2Factory.verify(password, row._4.passwordHash) then ZIO.succeed(resultsRaw)
            else ZIO.fail(GeneralError.IncorrectPassword)
          case None => ZIO.fail(GeneralError.EntityNotFound(userName))
        }
      )
    ).mapBoth(
      {
        case e: SQLException => GeneralError.Exception(e.getMessage)
        case e: GeneralError => e
      },
      list => this.mapUsers(list.map((m, r, p, c) => (m, r, p, Some(c)))).head
    )
  }

  def add(using quill: QuillCtx)(user: NewPasswordUser): IO[InsertDataError, User] = {
    import quill.*
    val members = user.to[Members]
    val creds = PasswordCredentials(
      user.id,
      user.credentials.userName,
      argon2Factory.hash(user.credentials.password)
    )
    transaction {
      for {
        _ <- run(query[Members].insertValue(lift(members)))
        _ <- run(query[PasswordCredentials].insertValue(lift(creds)))
        _ <- run {
          liftQuery(user.roles.map(r => Roles(r.id, r.name))).foreach(r =>
            query[RoleGrants].insertValue(RoleGrants(r.id, lift(user.id)))
          )
        }
      } yield (User(
        user.id,
        user.name,
        user.roles,
        Set(Login(creds.userName, LoginType.PasswordCredentials))
      ))
    }.tapError({
      case e: SQLException =>
        ZIO.logError(
          s"SQLException : ${e.getMessage()} : error code: ${e.getErrorCode()}, : sqlState: ${e.getSQLState()}"
        )
      case t: Throwable => ZIO.logError(s"Throwable : ${t.getMessage()} : class: ${t.getClass().toString()}")
    }).mapError({
      case e: SQLException if e.getErrorCode == 19 && e.getMessage().contains("user_name") =>
        InsertDataError.UniqueKeyViolation("userName")
      case t: Throwable => InsertDataError.Exception(t.getMessage)
    })
  }

  def update(using quill: QuillCtx)(user: User): IO[ExistingEntityError, User] = {
    import quill.*
    val member = user.to[Members]
    
    transaction {
      for {
        // Update member details
        _ <- run(query[Members].filter(m => m.id == lift(member.id)).updateValue(lift(member)))
        
        // Get current role IDs
        currentRoleIds <- run(query[RoleGrants].filter(rg => rg.memberId == lift(user.id)).map(_.roleId))
        
        // Calculate role IDs to add (new roles not in current)
        newRoleIds = user.roles.map(_.id).filterNot(rid => currentRoleIds.contains(RoleId.unwrap(rid)))
        // Calculate role IDs to remove (current roles not in new)
        roleIdsToRemove = currentRoleIds.filterNot(crid => user.roles.map(_.id).exists(rid => RoleId.unwrap(rid) == crid))
        
        // Insert new role grants
        _ <- run(
          liftQuery(newRoleIds).foreach(roleId =>
            query[RoleGrants].insertValue(RoleGrants(roleId, lift(user.id)))
          )
        )
        
        // Delete removed role grants
        _ <- run(
          query[RoleGrants]
            .filter(r => r.memberId == lift(user.id) && liftQuery(roleIdsToRemove).contains(r.roleId))
            .delete
        )
      } yield ()
    }.mapError({ case e: SQLException =>
      ZIO.logError(
        s"SQLException : ${e.getMessage()} : error code: ${e.getErrorCode()}, : sqlState: ${e.getSQLState()}"
      )
      ExistingEntityError.Exception(e.getMessage)
    }) *> getList(Some(user.id)).map(_.head)
  }

  private def queryJoinTables(using quill: QuillCtx)(m: Members) = {
    val quill = summon[QuillCtx]
    import quill.*
    for {
      rg    <- query[RoleGrants].leftJoin(rg => rg.memberId == lift(m.id))
      r     <- query[Roles].leftJoin(r => r.id == rg.orNull.roleId)
      pg    <- query[PermissionGrants].leftJoin(pg => pg.roleId == r.orNull.id)
      p     <- query[Permissions].leftJoin(p => p.id == pg.orNull.permissionId)
      creds <- query[PasswordCredentials].leftJoin(creds => creds.memberId == m.id)
    } yield (m, r, p, creds)
  }

  def getList(using quill: QuillCtx)(userIdOpt: Option[UserId]) =
    import quill.*
    run {
      for {
        m     <- query[Members].filter(m => lift(userIdOpt).filterIfDefined(_ == m.id))
        rg    <- query[RoleGrants].leftJoin(rg => rg.memberId == m.id)
        r     <- query[Roles].leftJoin(r => r.id == rg.orNull.roleId)
        pg    <- query[PermissionGrants].leftJoin(pg => pg.roleId == r.orNull.id)
        p     <- query[Permissions].leftJoin(p => p.id == pg.orNull.permissionId)
        creds <- query[PasswordCredentials].leftJoin(creds => creds.memberId == m.id)
      } yield (m, r, p, creds)
    }
      .fold(
        _ => List(),
        list => mapUsers(list)
      )

  def delete(using quill: QuillCtx)(id: UserId): IO[ErrorCode, Unit] =
    import quill.*
    run {
      query[Members].filter(m => m.id == lift(id)).delete
    }.mapBoth(e => GeneralError.Exception(e.getMessage), _ => ())

end UserRepositoryLive
