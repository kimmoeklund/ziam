package fi.kimmoeklund.service

import com.outr.scalapass.Argon2PasswordFactory
import fi.kimmoeklund.domain.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import io.github.arainko.ducktape.*
import zio.*

import java.sql.SQLException
import java.util.UUID

type UsersJoin = List[(Members, Option[Roles], Option[Permissions], Option[PasswordCredentials])]

final class UserRepositoryLive(
    quill: Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]],
    argon2Factory: Argon2PasswordFactory
) extends UserRepository
    with RepositoryUtils:
  import quill.*

  private def mapUsers(
      users: UsersJoin
  ) =
    val members     = users.map(_._1).distinct
    val permissions = users.groupMap(_._2.orNull)(_._3)
    val roles       = users.groupMap(_._1)(_._2)
    val creds       = users.groupMap(_._1)(_._4)
    members.map(m =>
      User(
        UserId(m.id),
        m.name,
        roles(m).flatten.distinct.map(r =>
          Role(
            RoleId(r.id),
            r.name,
            permissions(r).flatten.map(p => Permission(p.id, p.target, p.permission))
          )
        ).toSet,
        creds(m).flatten.distinct.map(c => Login(c.userName, LoginType.PasswordCredentials))
      )
    )

  override def checkUserPassword(
      userName: String,
      password: String
  ): IO[ErrorCode, User] = {
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

  override def addUser(user: NewPasswordUser): IO[InsertDataError, User] = {
    val members = user.into[Members].transform(Field.computed(_.id, u => UserId.unwrap(u.id)))
    val creds = PasswordCredentials(
      UserId.unwrap(user.id),
      user.credentials.userName,
      argon2Factory.hash(user.credentials.password)
    )
    transaction {
      for {
        _ <- run(query[Members].insertValue(lift(members)))
        _ <- run(query[PasswordCredentials].insertValue(lift(creds)))
        _ <- run {
          liftQuery(user.roles.map(toRoles)).foreach(r =>
            query[RoleGrants].insertValue(RoleGrants(r.id, lift(UserId.unwrap(user.id))))
          )
        }
      } yield (User(
        user.id,
        user.name,
        user.roles,
        Seq(Login(creds.userName, LoginType.PasswordCredentials))
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

  override def updateUser(user: User): IO[ExistingEntityError, User] = {
    val member     = user.into[Members].transform(Field.computed(_.id, u => UserId.unwrap(u.id)))
    val newRoleIds = user.roles.map(r => RoleId.unwrap(r.id))
    transaction {
      for {
        _              <- run(query[Members].filter(m => m.id == lift(member.id)).updateValue(lift(member)))
        currentRoleIds <- run(query[RoleGrants].filter(rg => rg.memberId == lift(UserId.unwrap(user.id))).map(_.roleId))
        _ <- run(
          liftQuery(newRoleIds.filterNot(r => currentRoleIds.contains(r))).foreach(r =>
            query[RoleGrants].insertValue(RoleGrants(r, lift(UserId.unwrap(user.id))))
          )
        )
        _ <- run(
          query[RoleGrants]
            .filter(r => liftQuery(currentRoleIds.filterNot(r2 => newRoleIds.contains(r2))).contains(r.roleId))
            .delete
        )
      } yield ()
    }.mapError({
      case e: SQLException =>
        ZIO.logError(
          s"SQLException : ${e.getMessage()} : error code: ${e.getErrorCode()}, : sqlState: ${e.getSQLState()}"
        )
        ExistingEntityError.Exception(e.getMessage)
    }) *> getUsers(Some(user.id)).map(_.head)
  }

  private val queryJoinTables = (m: Members) => {
    for {
      rg    <- query[RoleGrants].leftJoin(rg => rg.memberId == lift(m.id))
      r     <- query[Roles].leftJoin(r => r.id == rg.orNull.roleId)
      pg    <- query[PermissionGrants].leftJoin(pg => pg.roleId == r.orNull.id)
      p     <- query[Permissions].leftJoin(p => p.id == pg.orNull.permissionId)
      creds <- query[PasswordCredentials].leftJoin(creds => creds.memberId == m.id)
    } yield (m, r, p, creds)
  }

  override def getUsers(userIdOpt: Option[UserId]) = 
    run {
    for {
      m     <- query[Members].filter(m => lift(userIdOpt.map(UserId.unwrap)).filterIfDefined(_ == m.id))
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

  override def deleteUser(id: UserId): IO[ErrorCode, Unit] = run {
    query[Members].filter(m => m.id == lift(UserId.unwrap(id))).delete
  }.mapBoth(e => GeneralError.Exception(e.getMessage), _ => ())

end UserRepositoryLive

object UserRepositoryLive:
  def sqliteLayer(keys: Seq[String]): ZLayer[
    Map[String, Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]]] & Argon2PasswordFactory,
    Nothing,
    Map[String, UserRepository]
  ] = {
    val repos = ZIO
      .foreach(keys) { key =>
        for {
          quill         <- ZIO.serviceAt[Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]]](key)
          argon2Factory <- ZIO.service[Argon2PasswordFactory]
        } yield (key, UserRepositoryLive(quill.get, argon2Factory).asInstanceOf[UserRepository])
      }
      .map(t => ZEnvironment(t.toMap))
    ZLayer.fromZIOEnvironment(repos)
  }
