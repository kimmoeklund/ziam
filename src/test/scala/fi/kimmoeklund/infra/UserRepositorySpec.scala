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
import scala.collection.mutable.ListBuffer

final case class TestScenario(
    users: List[User],
    roles: List[Role],
    permissions: List[Permission],
    credentials: List[PasswordCredentials]
)

object TestScenario:
  def create: TestScenario = TestScenario(List(), List(), List(), List())

object UserRepositorySpec extends ZIOSpecDefault:

  val containerLayer = ZLayer.scoped(PostgresContainer.make())
  val dataSourceLayer = ZLayer(ZIO.service[DataSourceBuilder].map(_.dataSource))
  val postgresLayer =
    Quill.Postgres.fromNamingStrategy(NamingStrategy(SnakeCase, Escape))
  val repoLayer = UserRepositoryLive.layer
  val testScenario = ZState.initial(TestScenario.create)
  val unicodeString = Gen.stringBounded(5, 100)(Gen.unicodeChar)
  val asciiString = Gen.stringBounded(3, 12)(Gen.alphaNumericChar)

  def fetchUsers: ZIO[ZState[TestScenario] & UserRepository, Throwable, List[Spec[Any, Nothing]]] =
    val users = for {
      testData <- ZIO.serviceWithZIO[ZState[TestScenario]](_.get)
      userResults <- ZIO.collectAll(testData.credentials.map { creds =>
        UserRepository.checkUserPassword(
          creds.userName,
          creds.password
        )
      })
    } yield (userResults, testData.users)

    users.map((fetchedUsers, createdUsers) => {
      createdUsers.map(cUser => {
        test("it should assert fetched user matches with created") {
          val fUser = fetchedUsers.find(fu => fu.get.id == cUser.id)
          assertTrue(cUser != fUser)
        }
      })
    })

  override def spec = (
    suite("user repository test with postgres test container")(
      test("it should add permission to the database") {
        check(asciiString, Gen.int) { (name, number) =>
          val permission =
            Permission(UUID.randomUUID(), s"permission-$name", number)
          for {
            result <- UserRepository.addPermission(permission)
            testData <- ZIO.service[ZState[TestScenario]]
            _ <- testData.update(data =>
              data.copy(permissions = data.permissions :+ permission)
            )
          } yield assertTrue(result == ())
        }
      },
      test("it should add roles to the database") {
        check(asciiString) { name =>
          for {
            testState <- ZIO.service[ZState[TestScenario]]
            testData <- testState.get
            newRole <- ZIO.succeed(
              Role(UUID.randomUUID(), s"role-$name", testData.permissions)
            )
            role <- UserRepository.addRole(newRole)
            _ <- testState.update(data =>
              data.copy(roles = data.roles :+ newRole)
            )
          } yield assertTrue(role == ())
        }
      }, 
      test("it should add user to the database") {
        check(unicodeString, asciiString) { (randomName, userName) =>
          val newCreds =
            PasswordCredentials(UUID.randomUUID(), userName, userName)
          for {
            testState <- ZIO.service[ZState[TestScenario]]
            testData <- testState.get
            success <- UserRepository.addUser(
              User(newCreds.userId, randomName, testData.roles),
              newCreds,
              Organization(UUID.randomUUID(), "test org")
            )
            _ <- testState.update(data =>
              data.copy(credentials = data.credentials :+ newCreds)
            )
          } yield assertTrue(success == ())
        }
      },
    ) + suite("fetch users")(fetchUsers)).provideShared(containerLayer,
      DataSourceBuilderLive.layer,
      dataSourceLayer,
      postgresLayer,
      repoLayer,
      testScenario
    ) @@ sequential @@ samples(10) @@ nondeterministic
