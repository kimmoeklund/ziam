package fi.kimmoeklund.infra

import fi.kimmoeklund.domain.*
import fi.kimmoeklund.service.UserRepository
import io.getquill.*
import io.getquill.ast.SetOperator.isEmpty
import io.getquill.jdbczio.Quill
import io.github.arainko.ducktape.*
import org.postgresql.util.PSQLException
import zio.*

import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

case class Members(id: UUID, organization: UUID, name: String)
case class Memberships(memberId: UUID, parent: UUID)
case class Roles(id: UUID, name: String)
case class RoleGrants(roleId: UUID, memberId: UUID)
case class PermissionGrants(roleId: UUID, permissionId: UUID)
case class Permissions(id: UUID, target: String, permission: Int)
case class PasswordCredentials(
    memberId: UUID,
    userName: String,
    password: String
)

object Transformers:
  def toMember(user: NewPasswordUser): Members =
    user.into[Members].transform(Field.computed(_.organization, u => u.organization.id))
//  def toPasswordCredentialsTable(
//      creds: domain.NewPasswordCredentials
//  ): PasswordCredentials = creds.into[PasswordCredentials].transform(Field.renamed(_.memberId, _.userId))
  def toRoles(role: Role): Roles = role.to[Roles]

final class UserRepositoryLive(
    quill: Quill.Postgres[CompositeNamingStrategy2[SnakeCase.type, Escape.type]]
) extends UserRepository:

  import Transformers.*
  import quill.*

  private def mapUsers(
      users: List[(Members, Members, Option[Roles], Option[Permissions], Option[PasswordCredentials])]
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
            r.id,
            r.name,
            permissions(r).flatten.map(p => Permission(p.id, p.target, p.permission))
          )
        ),
        creds(m).flatten.distinct.map(c => Login(c.userName, LoginType.PasswordCredentials))
      )
    )

  private def mapRoles(roles: List[(Roles, Option[PermissionGrants], Option[Permissions])]) =
    val permissionsMap = roles.groupMap(_._1)(_._3)
    roles
      .map(r =>
        Role(
          r._1.id,
          r._1.name,
          permissionsMap(r._1).flatten.map(p => Permission(p.id, p.target, p.permission))
        )
      )
      .distinct

  override def checkUserPassword(
      userName: String,
      password: String
  ): IO[ErrorCode, Option[User]] = {
    run {
      for {
        creds <- query[PasswordCredentials].filter(p => p.userName == lift(userName) && p.password == lift(password))
        m <- query[Members].join(m => m.id == creds.memberId)
        o <- query[Members].join(o => o.id == m.organization)
        rg <- query[RoleGrants].leftJoin(rg => rg.memberId == m.id)
        r <- query[Roles].leftJoin(r => r.id == rg.orNull.roleId)
        pg <- query[PermissionGrants].leftJoin(pg => pg.roleId == r.orNull.id)
        p <- query[Permissions].leftJoin(p => p.id == pg.orNull.permissionId)
      } yield (m, o, r, p, creds)

    }.fold(
      _ => Option.empty,
      list =>
        val user = mapUsers(list.map((m, o, r, p, c) => (m, o, r, p, Some(c))))
        if (user.isEmpty) Option.empty else Some(user.head)
    )
  }

  override def updateUserRoles: IO[ErrorCode, Option[User]] = ???
  override def effectivePermissionsForUser(
      id: String
  ): IO[ErrorCode, Option[Seq[Permission]]] = ???

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
    }.mapBoth(_ => GeneralErrors.Exception, _ => org)

  override def addUser(user: NewPasswordUser): IO[ErrorCode, User] = {
    val members = toMember(user)
    val creds = PasswordCredentials(
      user.id,
      user.credentials.userName,
      user.credentials.password
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
      case e: PSQLException if e.getSQLState() == "23505" && e.getMessage().contains("user_name") =>
        GeneralErrors.UniqueKeyViolation("userName")
      case t: Throwable => GeneralErrors.Exception
    })
  }

  override def addRole(role: Role): IO[ErrorCode, Role] =
    val newRole = Roles(role.id, role.name)
    transaction {
      for {
        _ <-
          run(query[Roles].insertValue(lift(newRole)))
        _ <-
          run {
            liftQuery(role.permissions).foreach(p =>
              query[PermissionGrants].insertValue(
                PermissionGrants(lift(role.id), p.id)
              )
            )
          }
      } yield (newRole)
    }.mapBoth(_ => GeneralErrors.Exception, r => r.into[Role].transform(Field.const(_.permissions, role.permissions)))

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
    } yield (permission)).mapBoth(_ => GeneralErrors.Exception, p => p)

  override def getUsers: IO[ErrorCode, List[User]] = run {
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
  override def getPermissions: IO[ErrorCode, List[Permission]] =
    val users = run {
      query[Permissions]
    }.fold(
      _ => List(),
      list => list.map(p => Permission(p.id, p.target, p.permission))
    )
    users

  override def getPermissionsById(ids: Seq[UUID]): IO[ErrorCode, List[Permission]] = run {
    quote { query[Permissions].filter(p => liftQuery(ids).contains(p.id)) }
  }.map(l => l.map(p => p.to[Permission]))
    .filterOrElseWith(permissions => permissions.size == ids.size)(permissions =>
      ZIO.fail(GeneralErrors.EntityNotFound(ids.diff(permissions.map(_.id)).mkString(",")))
    )
    .mapError({
      case _: SQLException  => GeneralErrors.Exception
      case e: GeneralErrors => e
    })

  override def deletePermission(id: UUID): IO[ErrorCode, Unit] = {
    run {
      query[Permissions].filter(p => p.id == lift(id)).delete
    }.mapBoth(_ => GeneralErrors.Exception, _ => ())
  }

  override def deleteRole(id: UUID): IO[ErrorCode, Unit] =
    run {
      query[Roles].filter(r => r.id == lift(id)).delete
    }.mapBoth(_ => GeneralErrors.Exception, _ => ())

  override def getRoles: IO[ErrorCode, List[Role]] = {
    run {
      for {
        roles <- query[Roles]
        pg <- query[PermissionGrants].leftJoin(pg => roles.id == pg.roleId)
        p <- query[Permissions].leftJoin(p => pg.forall(grant => p.id == grant.permissionId))
      } yield (roles, pg, p)
    }.fold(
      _ => List(),
      list => mapRoles(list)
    )
  }

  override def getRolesByIds(ids: Seq[UUID]): IO[ErrorCode, List[Role]] = run {
    for {
      roles <- query[Roles].filter(r => liftQuery(ids).contains(r.id))
      pg <- query[PermissionGrants].leftJoin(pg => roles.id == pg.roleId)
      p <- query[Permissions].leftJoin(p => p.id == pg.orNull.permissionId)
    } yield (roles, pg, p)
  }.map(list => mapRoles(list))
    .filterOrElseWith(roles => roles.size == ids.size)(roles =>
      ZIO.fail(GeneralErrors.EntityNotFound(ids.diff(roles.map(_.id)).mkString(",")))
    )
    .mapError({
      case _: SQLException  => GeneralErrors.Exception
      case e: GeneralErrors => e
    })

  override def getOrganizations: IO[ErrorCode, List[Organization]] =
    run {
      query[Members].filter(m => m.id == m.organization)
    }.fold(
      _ => List(),
      list => list.map(m => Organization(m.id, m.name))
    )

  override def deleteOrganization(id: UUID): IO[ErrorCode, Unit] =
    run {
      query[Members].filter(o => o.id == lift(id)).delete
    }.mapBoth(_ => GeneralErrors.Exception, _ => ())

  override def getOrganizationById(id: UUID): IO[ErrorCode, Organization] =
    run {
      query[Members].filter(o => o.id == lift(id) && o.id == o.organization)
    }.mapError(e => GeneralErrors.Exception)
      .filterOrFail(_.nonEmpty)(GeneralErrors.EntityNotFound(id.toString))
      .map(list => list.map(m => Organization(m.id, m.name)).head)

  override def deleteUser(id: UUID): IO[ErrorCode, Unit] = run {
    query[Members].filter(m => m.id == lift(id)).delete
  }.mapBoth(_ => GeneralErrors.Exception, _ => ())

end UserRepositoryLive

object UserRepositoryLive:
  def layer: URLayer[Quill.Postgres[
    CompositeNamingStrategy2[SnakeCase.type, Escape.type]
  ], UserRepository] = ZLayer {
    for {
      quill <- ZIO.service[Quill.Postgres[
        CompositeNamingStrategy2[SnakeCase.type, Escape.type]
      ]]
    } yield UserRepositoryLive(quill)
  }
