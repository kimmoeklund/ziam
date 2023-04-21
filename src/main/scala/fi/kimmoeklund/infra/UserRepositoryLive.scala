package fi.kimmoeklund.infra

import java.util.UUID
import fi.kimmoeklund.service.UserRepository
import javax.sql.DataSource
import zio.*
import io.getquill.jdbczio.Quill
import io.getquill.NamingStrategy
import io.getquill.*
import fi.kimmoeklund.domain.User
import fi.kimmoeklund.domain.Role
import fi.kimmoeklund.domain.Permission
import fi.kimmoeklund.domain.Organization

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
// refactor with chimney

object Conv:
  def toMember(user: User, org: Organization): Members =
    new Members(user._1, org._1, user._2)
  def toPasswordCredentialsTable(
      user: User,
      creds: fi.kimmoeklund.domain.PasswordCredentials
  ) =
    new PasswordCredentials(user._1, creds._2, creds._3)
  def toRoles(role: Role) = Roles(role.id, role.name)

final class UserRepositoryLive(
    quill: Quill.Postgres[CompositeNamingStrategy2[SnakeCase.type, Escape.type]]
) extends UserRepository:

  import quill.*

  override def checkUserPassword(
      userName: String,
      password: String
  ): Task[Option[User]] = {
    return run {
      quote {
        for {
          creds <- query[PasswordCredentials].filter(p =>
            p.userName == lift(userName) && p.password == lift(password)
          )
          m <- query[Members].join(m => m.id == creds.memberId)
          rg <- query[RoleGrants].leftJoin(rg => rg.memberId == m.id)
          r <- query[Roles].leftJoin(r =>
            rg.forall(grant => r.id == grant.roleId)
          )
          pg <- query[PermissionGrants].leftJoin(pg =>
            r.forall(role => role.id == pg.roleId)
          )
          p <- query[Permissions].leftJoin(p =>
            pg.forall(grant => p.id == grant.permissionId)
          )
        } yield (m, r, pg, p)
      }
    }.fold(
      _ => Option.empty,
      list => {
        val roles = list.flatMap(_._2).distinct
        val permissionGrants =
          list.flatMap(_._3).groupMap(_.roleId)(_.permissionId)
        val permissions = list.flatMap(_._4).distinct
        val domainRoles = roles
          .map(r =>
            new Role(
              r.id,
              r.name,
              permissions
                .filter(permission =>
                  permissionGrants(r.id).contains(permission.id)
                )
                .map(p => new Permission(p.id, p.target, p.permission))
            )
          )
        Some(new User(list.head._1.id, list.head._1.name, domainRoles))
      }
    )
  }

  override def updateUserRoles(): Task[Option[User]] = ???
  override def effectivePermissionsForUser(
      id: String
  ): Task[Option[Seq[Permission]]] = ???

  override def addUser(
      user: User,
      pwdCredentials: fi.kimmoeklund.domain.PasswordCredentials,
      organization: Organization
  ): Task[Unit] = {
    val members = Conv.toMember(user, organization)
    val creds = Conv.toPasswordCredentialsTable(user, pwdCredentials)
    val ret =
      transaction {
        for {
          _ <- run(query[Members].insertValue(lift(members)))
          _ <- run(query[PasswordCredentials].insertValue(lift(creds)))
          _ <- run {
            liftQuery(user.roles.map(Conv.toRoles)).foreach(r =>
              query[RoleGrants].insertValue(RoleGrants(r.id, lift(user.id)))
            )
          }
        } yield ()
      }
    return ret
  }
  override def addRole(role: Role): Task[Unit] =
    val ret = transaction {
      for {
        _ <-
          run(query[Roles].insertValue(lift(new Roles(role.id, role.name))))
        _ <-
          run {
            liftQuery(role.permissions).foreach(p =>
              query[PermissionGrants].insertValue(
                new PermissionGrants(lift(role.id), p.id)
              )
            )
          }
      } yield ()
    }
    return ret

  override def addPermission(permission: Permission) =
    for {
      _ <- run(
        query[Permissions].insertValue(
          lift(
            new Permissions(
              permission.id,
              permission.target,
              permission.permission
            )
          )
        )
      )
    } yield ()

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
