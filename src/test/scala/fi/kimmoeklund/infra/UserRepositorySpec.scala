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

  val user = User(UUID.randomUUID(), "test user", Seq.empty)
  val org = Organization(UUID.randomUUID(), "test org")
  val creds = PasswordCredentials(user._1, "test@email.invalid", "password")

  override def spec =
    suite("user repository test with postgres test container")(
      test("it should add user to the database") {
        for {
          success <- UserRepository.add(user, creds, org)
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
