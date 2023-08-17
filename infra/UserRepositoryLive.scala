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
  inline def fetchUserDetails(members: Members) =
    // val memberList = List(members)
    for {
      rg <- query[RoleGrants].filter(rg => rg.memberId == members.id
//        liftQuery(memberList).contains(rg.memberId)
      )
      r <- query[Roles].leftJoin(r => r.id == rg.roleId)
      pg <- query[PermissionGrants].leftJoin(pg =>
        r.forall(role => role.id == pg.roleId)
      )
      p <- query[Permissions].leftJoin(p =>
        pg.forall(grant => p.id == grant.permissionId)
      )
    } yield (r, p)

  def mapToDomain(list: List[(Members, Option[Roles], Option[Permissions])]) =
    val roles = list.flatMap(_._2).distinct
    val permissions = list.flatMap(_._3).distinct
    val domainRoles = roles
      .map(r =>
        new Role(
          r.id,
          r.name,
          permissions
            .map(p => new Permission(p.id, p.target, p.permission))
        )
      )
    new User(list.head._1.id, list.head._1.name, domainRoles)

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
          (r, p) <- fetchUserDetails(m)
        } yield (m, r, p)
      }
    }.fold(
      _ => Option.empty,
      result => Some(mapToDomain(result))
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
    val members = Transformers.toMember(user, organization)
    val creds = Transformers.toPasswordCredentialsTable(user, pwdCredentials)
    val ret =
      transaction {
        for {
          _ <- run(query[Members].insertValue(lift(members)))
          _ <- run(query[PasswordCredentials].insertValue(lift(creds)))
          _ <- run {
            liftQuery(user.roles.map(Transformers.toRoles)).foreach(r =>
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

  override def getUsers(): Task[List[User]] = {
    val q = run {
      quote {
        for {
          m <- query[Members]
          (p, r) <- fetchUserDetails(m)        
        } yield (m, p, r)
      }
    }.map(items => {
      val groupedByUsers = items.groupBy(_._1.id)
      val users = groupedByUsers.keySet.map(key => mapToDomain(groupedByUsers(key))).toList
      users
    }).fold(_ => List(), users => users)
    return q
  }

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
