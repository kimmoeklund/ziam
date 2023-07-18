package fi.kimmoeklund.infra

import fi.kimmoeklund.domain
import io.getquill.PluralizedTableNames
import io.getquill.jdbczio.Quill
import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import io.getquill.PluralizedTableNames
import fi.kimmoeklund.domain.*

import java.util.UUID
import fi.kimmoeklund.service.UserRepository
import io.getquill.SnakeCase
import io.getquill.Escape
import io.getquill.NamingStrategy
import org.junit.Assert

import scala.collection.mutable.ListBuffer

final case class TestScenario(
    organization: Organization,
    users: List[User],
    roles: List[Role],
    permissions: List[Permission],
)

val testOrg = Organization(UUID.randomUUID(), "test org")

object TestScenario:
  def create: TestScenario = TestScenario(testOrg,List(), List(), List())

object UserRepositorySpec extends ZIOSpecDefault:

  val containerLayer = ZLayer.scoped(PostgresContainer.make())
  val dataSourceLayer = ZLayer(ZIO.service[DataSourceBuilder].map(_.dataSource))
  val postgresLayer =
    Quill.Postgres.fromNamingStrategy(NamingStrategy(SnakeCase, Escape))
  val repoLayer = UserRepositoryLive.layer
  val testScenario = ZState.initial(TestScenario.create)
  val unicodeString = Gen.stringBounded(5, 100)(Gen.unicodeChar)
  val asciiString = Gen.stringBounded(3, 12)(Gen.alphaNumericChar)

  def fetchUsers: ZIO[ZState[TestScenario] & UserRepository, Throwable, List[
    Spec[Any, Nothing]
  ]] =
    val users = for {
      testData <- ZIO.serviceWithZIO[ZState[TestScenario]](_.get)
      userResults <- ZIO.collectAll(testData.users.map { user =>
        for {
          //_ <- Console.printLine(s"fetching user ${creds.userId}")
          user <- UserRepository.checkUserPassword(
          user.logins.head.userName,
          user.logins.head.userName
          )
        } yield user
      })
    } yield (userResults, testData.users, testData.roles)

    users.map((fetchedUsers, createdUsers, createdRoles) => {
      createdUsers.map(cUser => {
        test("it should check password for created user") {
          val fUser = fetchedUsers.find(fu => fu.get.id == cUser.id)
          assertTrue(cUser._1 == fUser.orNull.orNull._1)
          assertTrue(cUser._2 == fUser.orNull.orNull._2)
          assertTrue(fUser.orNull.orNull.roles.forall(fRole => {
            val cRole = createdRoles.find(cr => cr._1 == fRole._1).get
            cRole.permissions.sorted == fRole.permissions.sorted
          }))
        }
      })
    })

  override def spec =
    (suite("user repository test with postgres test container")(
      test("it should add permission to the database") {
        check(asciiString, Gen.int) { (name, number) =>
          val permission =
            Permission(UUID.randomUUID(), s"permission-$name", number)
          for {
            result <- UserRepository.addPermission(permission)
            testData <- ZIO.service[ZState[TestScenario]]
            _ <- testData.update(data => data.copy(permissions = data.permissions :+ permission))
          } yield assertTrue(result == permission)
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
            _ <- testState.update(data => data.copy(roles = data.roles :+ newRole))
          } yield assertTrue(role == newRole)
        }
      },
      test("it should add organization to the database") {
        for {
          testState <- ZIO.service[ZState[TestScenario]]
          testData <- testState.get
          org <- UserRepository.addOrganization(testData.organization)
        } yield assertTrue(org == testData.organization)
      },
      test("it should add user to the database") {
        check(unicodeString, asciiString) { (randomName, userName) => {
          val userId = UUID.randomUUID()
          for {
            testState <- ZIO.service[ZState[TestScenario]]
            testData <- testState.get
            newPassword <- ZIO.fromOption(NewPasswordCredentials(userId, userName, userName, userName))
            success <- UserRepository.addUser(NewPasswordUser(userId, randomName, testOrg, newPassword, testData.roles))
            _ <- testState.update(data =>
              data.copy(
                users = data.users :+ success
              )
            )
          } yield assertTrue(success.id == newPassword.userId)
          }
        }
      },
      test("it should get and delete permission from the database") {
        for {
            permission <- UserRepository.addPermission(Permission(UUID.randomUUID(), "test permission", 1))
            _ <- UserRepository.deletePermission(permission.id)
            allPermissions <- UserRepository.getPermissions()
        } yield assertTrue(!allPermissions.exists(p => p.id == permission.id))
      },
      test(label = "it should get permissions by id") {
        for {
          testState <- ZIO.service[ZState[TestScenario]]
          testData <- testState.get
          permissions <- UserRepository.getPermissionsById(testData.permissions.map(p => p.id))
        } yield assertTrue(permissions == testData.permissions)
      },
      test("it should delete roles from the database") {
        for {
          testState <- ZIO.service[ZState[TestScenario]]
          testData <- testState.get
          role <- UserRepository.addRole(Role(UUID.randomUUID(), "test role", testData.permissions))
          _ <- UserRepository.deleteRole(role.id)
          allRoles <- UserRepository.getRoles()
        } yield assertTrue(!allRoles.exists(r => r.id == role.id))
      },
      test(label = "it should delete organization") {
        for {
          org <- UserRepository.addOrganization(Organization(UUID.randomUUID(), "test org2"))
            _ <- UserRepository.deleteOrganization(org.id)
          allOrgs <- UserRepository.getOrganizations()
        } yield assertTrue(!allOrgs.exists(o => o.id == org.id))
      },
    ) + suite("fetch users")(fetchUsers)).provideShared(
      containerLayer,
      DataSourceBuilderLive.layer,
      dataSourceLayer,
      postgresLayer,
      repoLayer,
      testScenario
    ) @@ sequential @@ samples(1) @@ nondeterministic
