package fi.kimmoeklund.infra

import fi.kimmoeklund.domain
import fi.kimmoeklund.domain.{Organization, Permission, Role, User, Login, LoginType}

import java.util.UUID
import fi.kimmoeklund.service.UserRepository

import javax.sql.DataSource
import zio.*
import io.getquill.jdbczio.Quill
import io.getquill.NamingStrategy
import io.getquill.*
import io.github.arainko.ducktape.*

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
  def toMember(user: User): Members =
    user.into[Members].transform(Field.computed(_.organization, u => u.organization.id))
  def toPasswordCredentialsTable(
      creds: domain.PasswordCredentials
  ): PasswordCredentials = creds.into[PasswordCredentials].transform(Field.renamed(_.memberId, _.userId))
  def toRoles(role: Role): Roles = role.to[Roles]

final class UserRepositoryLive(
    quill: Quill.Postgres[CompositeNamingStrategy2[SnakeCase.type, Escape.type]]
) extends UserRepository:

  import quill.*

  private def userJoinQuote = quote { (m: Members) =>
    for {
      o <- query[Organization].join(o => o.id == m.organization)
      rg <- query[RoleGrants].leftJoin(rg => rg.memberId == m.id)
      r <- query[Roles].leftJoin(r => rg.forall(grant => r.id == grant.roleId))
      pg <- query[PermissionGrants].leftJoin(pg => r.forall(role => role.id == pg.roleId))
      p <- query[Permissions].leftJoin(p => pg.forall(grant => p.id == grant.permissionId))
    } yield (o, rg, r, p)
  }

  override def checkUserPassword(
      userName: String,
      password: String
  ): Task[Option[User]] = {
    run {
      quote {
        for {
          creds <- query[PasswordCredentials].filter(p => p.userName == lift(userName) && p.password == lift(password))
          m <- query[Members].join(m => m.id == creds.memberId)
          (o, pg, r, p) <- userJoinQuote(m)
        } yield (m, o, r, p, creds)
      }
    }.fold(
      _ => Option.empty,
      list => {
        val organization = list.head._2
        val roles = list.flatMap(_._3).distinct
        val permissions = list.flatMap(_._4).distinct
        val domainRoles = roles
          .map(r =>
            new Role(
              r.id,
              r.name,
              permissions
                .map(p => new Permission(p.id, p.target, p.permission))
            )
          )
        val passwordCredentials = list.head._5
        Some(
          new User(
            list.head._1.id,
            list.head._1.name,
            Organization(UUID.randomUUID(), "todo"),
            domainRoles,
            Seq(Login(passwordCredentials.userName, LoginType.PasswordCredentials))
          )
        )
      }
    )
  }

  override def updateUserRoles(): Task[Option[User]] = ???
  override def effectivePermissionsForUser(
      id: String
  ): Task[Option[Seq[Permission]]] = ???

  override def addUser(
      user: User,
      pwdCredentials: domain.PasswordCredentials
  ): Task[Unit] = {
    val members = Transformers.toMember(user)
    val creds = Transformers.toPasswordCredentialsTable(pwdCredentials)
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
            Permissions(
              permission.id,
              permission.target,
              permission.permission
            )
          )
        )
      )
    } yield (permission)

  override def getUsers: Task[List[User]] =
    val users = run {
      quote {
        for {
          m <- query[Members]
          (o, rg, r, p) <- userJoinQuote(m)
          creds <- query[PasswordCredentials].leftJoin(creds => creds.memberId == m.id)
        } yield (m, o, rg, p, r, creds)
      }
    }.fold(
      _ => List(),
      list =>
        val members = list.map(_._1).distinct
        val permissions = list.groupMap(_._5.get)(_._4)
        val roles = list.groupMap(_._1)(_._5)
        val creds = list.groupMap(_._1)(_._6)
        members.map(m =>
          User(
            m.id,
            m.name,
            Organization(UUID.randomUUID(), "todo"),
            roles(m).flatMap(r =>
              r match {
                case Some(r) =>
                  List(
                    Role(
                      r.id,
                      r.name,
                      permissions(r).flatMap(p =>
                        p match {
                          case Some(p) =>
                            List(Permission(p.id, p.target, p.permission))
                          case None => List()
                        }
                      )
                    )
                  )
                case None => List()
              }
            ),
            creds(m).flatMap({
              case Some(c) => Seq(Login(c.userName, LoginType.PasswordCredentials))
              case None    => Seq()
            })
          )
        )
    )
    return users

  override def getPermissions: Task[List[Permission]] =
    val users = run {
      quote {
        query[Permissions]
      }
    }.fold(
      _ => List(),
      list => list.map(p => Permission(p.id, p.target, p.permission))
    )
    users

  override def deletePermission(id: UUID): Task[Unit] = {
    run {
      quote {
        query[Permissions].filter(p => p.id == lift(id)).delete
      }
    }.map(_ => ())
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
