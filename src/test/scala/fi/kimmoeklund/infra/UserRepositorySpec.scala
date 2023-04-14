package fi.kimmoeklund.infra

import io.getquill.PluralizedTableNames
import io.getquill.jdbczio.Quill
import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._
import io.getquill.PluralizedTableNames
import fi.kimmoeklund.domain._
import java.util.UUID
import fi.kimmoeklund.service.UserRepository
import io.getquill.SnakeCase
import io.getquill.Escape
import io.getquill.NamingStrategy

object UserRepositorySpec extends ZIOSpecDefault:

  val containerLayer = ZLayer.scoped(PostgresContainer.make())
  val dataSourceLayer = ZLayer(ZIO.service[DataSourceBuilder].map(_.dataSource))
  val postgresLayer = Quill.Postgres.fromNamingStrategy(NamingStrategy(SnakeCase, Escape))
  val repoLayer = UserRepositoryLive.layer

  val permission1 = Permission(UUID.randomUUID(), "test1", 1)
  val permission2 = Permission(UUID.randomUUID(), "test2", 1)
  val role1 = Role(UUID.randomUUID(), "testrole1", Seq(permission1))
  val role2 = Role(UUID.randomUUID(), "testrole2", Seq(permission2))
  val org = Organization(UUID.randomUUID(), "test org")
  val user = User(UUID.randomUUID(), "test user", Seq(role1, role2))
  val creds = PasswordCredentials(user._1, "test@email.invalid", "password")

  // TODO generators, and looping items and asserts

  override def spec =
    suite("user repository test with postgres test container")(
      test("it should add permission to the database") {
        for {
          permission1 <- UserRepository.addPermission(permission1)
          permission2 <- UserRepository.addPermission(permission2)
        } yield assert(permission2)(isUnit)
      },
      test("it should add roles to the database") {
        for {
          role1 <- UserRepository.addRole(role1)
          role2 <- UserRepository.addRole(role2)
        } yield assert(role2)(isUnit)
      },
      test("it should add user to the database") {
        for {
          success <- UserRepository.addUser(user, creds, org)
        } yield assert(success)(isUnit)
      },
      test("it should check user's password") {
        for {
          ret <- UserRepository.checkUserPassword(
            creds.userName,
            creds.password
          )
        } yield assert(ret)(equalTo(Some(user)))
      }
    ).provideShared(
      containerLayer,
      DataSourceBuilderLive.layer,
      dataSourceLayer,
      postgresLayer,
      repoLayer
    ) @@ sequential
