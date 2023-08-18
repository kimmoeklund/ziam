package fi.kimmoeklund.service

import com.outr.scalapass.Argon2PasswordFactory
import fi.kimmoeklund.domain.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import io.github.arainko.ducktape.*
import zio.*

import java.sql.SQLException
import java.util.UUID

type UsersJoin = List[(Members, Members, Option[Roles], Option[Permissions], Option[PasswordCredentials])]

final class UserRepositoryLive(
    quill: Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]],
    argon2Factory: Argon2PasswordFactory
) extends UserRepository
    with RepositoryUtils:
  import quill.*

  private def mapUsers(
      users: UsersJoin
  ) =
    val members = users.map(_._1).distinct
    val organizations = users.groupMap(_._1)(_._2)
    val permissions = users.groupMap(_._3.orNull)(_._4)
    val roles = users.groupMap(_._1)(_._3)
    val creds = users.groupMap(_._1)(_._5)
    members.map(m =>
      User(
        m.id,
        m.name,
        organizations(m).head.to[Organization],
        roles(m).flatten.distinct.map(r =>
          Role(
            RoleId(r.id),
            r.name,
            permissions(r).flatten.map(p => Permission(p.id, p.target, p.permission))
          )
        ),
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
        m <- query[Members].join(m => m.id == creds.memberId)
        o <- query[Members].join(o => o.id == m.organization)
        rg <- query[RoleGrants].leftJoin(rg => rg.memberId == m.id)
        r <- query[Roles].leftJoin(r => r.id == rg.orNull.roleId)
        pg <- query[PermissionGrants].leftJoin(pg => pg.roleId == r.orNull.id)
        p <- query[Permissions].leftJoin(p => p.id == pg.orNull.permissionId)
      } yield (m, o, r, p, creds)
    }.flatMap(resultsRaw =>
      ZIO.blocking(
        resultsRaw.headOption match {
          case Some(row) =>
            if argon2Factory.verify(password, row._5.passwordHash) then ZIO.succeed(resultsRaw)
            else ZIO.fail(GeneralError.IncorrectPassword)
          case None => ZIO.fail(GeneralError.EntityNotFound(userName))
        }
      )
    ).mapBoth(
      {
        case e: SQLException => GeneralError.Exception(e.getMessage)
        case e: GeneralError => e
      },
      list => this.mapUsers(list.map((m, o, r, p, c) => (m, o, r, p, Some(c)))).head
    )
  }

  override def effectivePermissionsForUser(
      id: String
  ): IO[ErrorCode, Option[Seq[Permission]]] = ???

  override def getOrganizations: IO[GetDataError, List[Organization]] =
    run {
      query[Members].filter(m => m.id == m.organization)
    }.fold(
      _ => List(),
      list => list.map(m => Organization(m.id, m.name))
    )

  override def addOrganization(org: Organization): IO[ErrorCode, Organization] =
    run {
      query[Members].insertValue(
        lift(
          Members(
            org.id,
            org.id,
            org.name
          )
        )
      )
    }.mapBoth(e => GeneralError.Exception(e.getMessage), _ => org)

  override def addUser(user: NewPasswordUser): IO[InsertDataError, User] = {
    val members = toMember(user)
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
          liftQuery(user.roles.map(toRoles)).foreach(r =>
            query[RoleGrants].insertValue(RoleGrants(r.id, lift(user.id)))
          )
        }
      } yield (User(
        user.id,
        user.name,
        user.organization,
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

  override def getUsers = run {
    for {
      m <- query[Members].filter(m => m.organization != m.id)
      o <- query[Members].join(o => o.organization == m.organization)
      rg <- query[RoleGrants].leftJoin(rg => rg.memberId == m.id)
      r <- query[Roles].leftJoin(r => r.id == rg.orNull.roleId)
      pg <- query[PermissionGrants].leftJoin(pg => pg.roleId == r.orNull.id)
      p <- query[Permissions].leftJoin(p => p.id == pg.orNull.permissionId)
      creds <- query[PasswordCredentials].leftJoin(creds => creds.memberId == m.id)
    } yield (m, o, r, p, creds)
  }
    .fold(
      _ => List(),
      list => mapUsers(list)
    )
  override def deleteOrganization(id: UUID): IO[ErrorCode, Unit] =
    run {
      query[Members].filter(o => o.id == lift(id)).delete
    }.mapBoth(e => GeneralError.Exception(e.getMessage), _ => ())

  override def getOrganizationById(id: UUID): IO[GetDataError, Organization] =
    run {
      query[Members].filter(o => o.id == lift(id) && o.id == o.organization)
    }.mapError(e => GetDataError.Exception(e.getMessage))
      .filterOrFail(_.nonEmpty)(GetDataError.EntityNotFound(id.toString))
      .map(list => list.map(m => Organization(m.id, m.name)).head)

  override def deleteUser(id: UUID): IO[ErrorCode, Unit] = run {
    query[Members].filter(m => m.id == lift(id)).delete
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
          quill <- ZIO.serviceAt[Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]]](key)
          argon2Factory <- ZIO.service[Argon2PasswordFactory]
        } yield (key, UserRepositoryLive(quill.get, argon2Factory).asInstanceOf[UserRepository])
      }
      .map(t => ZEnvironment(t.toMap))
    ZLayer.fromZIOEnvironment(repos)
  }
