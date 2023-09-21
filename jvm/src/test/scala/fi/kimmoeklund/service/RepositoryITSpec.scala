package fi.kimmoeklund.service

import com.outr.scalapass.Argon2PasswordFactory
import fi.kimmoeklund.domain.*
import zio.*
import zio.test.*
import zio.test.TestAspect.*

import java.util.UUID

final case class TestScenario(
    organization: Organization,
    users: List[User],
    roles: List[Role],
    permissions: List[Permission]
)

val testOrg = Organization(UUID.randomUUID(), "test org")

object TestScenario:
  def create: TestScenario = TestScenario(testOrg, List(), List(), List())

object UserRepositorySpec extends ZIOSpecDefault:

  val unicodeString =
    Gen.stringBounded(5, 100)(Gen.unicodeChar.filter(c => c != 0x00.toChar && !c.isControl && !c.isWhitespace))
  val asciiString = Gen.stringBounded(3, 12)(Gen.alphaNumericChar)
  val envKeys = Vector("unittest")

  def fetchUsers: ZIO[ZState[TestScenario] & Map[String, UserRepository], ErrorCode, List[
    Spec[Any, Nothing]
  ]] = {
    val users = for {
      testData <- ZIO.serviceWithZIO[ZState[TestScenario]](_.get)
      userResults <- ZIO.collectAll(testData.users.map { user =>
        for {
          repo <- ZIO.serviceAt[UserRepository]("unittest")
          user <- repo.get.checkUserPassword(
            user.logins.head.userName,
            user.logins.head.userName
          )
        } yield user
      })
    } yield (userResults, testData.users, testData.roles)

    users.map((fetchedUsers, createdUsers, createdRoles) => {
      createdUsers.map(cUser => {
        test("it should check password for created user") {
          val fUser = fetchedUsers.find(fu => fu.id == cUser.id)
          assertTrue(cUser._1 == fUser.orNull._1)
          assertTrue(cUser._2 == fUser.orNull._2)
          assertTrue(fUser.orNull.roles.forall(fRole => {
            val cRole = createdRoles.find(cr => cr._1 == fRole._1).get
            cRole.permissions.sorted == fRole.permissions.sorted
          }))
        }
      })
    })
  }

  override def spec = {
    (suite("Repository integration tests")(
      test("it should add permission to the database") {
        check(asciiString, Gen.int) { (name, number) =>
          val permission =
            Permission(UUID.randomUUID(), s"permission-$name", number)
          for {
            repo <- ZIO.serviceAt[PermissionRepository]("unittest")
            result <- repo.get.addPermission(permission)
            testData <- ZIO.service[ZState[TestScenario]]
            _ <- testData.update(data => data.copy(permissions = data.permissions :+ permission))
          } yield assertTrue(result == permission)
        }
      },
      test("it should add roles to the database") {
        check(asciiString) { name =>
          for {
            repo <- ZIO.serviceAt[RoleRepository]("unittest")
            testState <- ZIO.service[ZState[TestScenario]]
            testData <- testState.get
            newRole <- ZIO.succeed(
              Role(RoleId(UUID.randomUUID()), s"role-$name", testData.permissions)
            )
            role <- repo.get.addRole(newRole)
            _ <- testState.update(data => data.copy(roles = data.roles :+ newRole))
          } yield assertTrue(role == newRole)
        }
      },
      test("it should add organization to the database") {
        for {
          repo <- ZIO.serviceAt[UserRepository]("unittest")
          testState <- ZIO.service[ZState[TestScenario]]
          testData <- testState.get
          org <- repo.get.addOrganization(testData.organization)
        } yield assertTrue(org == testData.organization)
      },
      test("it should add user to the database") {
        check(unicodeString, asciiString) { (randomName, userName) =>
          {
            val userId = UUID.randomUUID()
            for {
              repo <- ZIO.serviceAt[UserRepository]("unittest")
              testState <- ZIO.service[ZState[TestScenario]]
              testData <- testState.get
              newPassword <- NewPasswordCredentials.fromOptions(Some(userName), Some(userName), Some(userName)).toZIO
              success <- repo.get.addUser(
                NewPasswordUser(userId, randomName, testOrg, newPassword, testData.roles)
              )
              _ <- testState.update(data =>
                data.copy(
                  users = data.users :+ success
                )
              )
            } yield assertTrue(success.logins.size == 1 && success.logins.head.userName == userName)
          }
        }
      },
      test("it should get and delete permission from the database") {
        for {
          repo <- ZIO.serviceAt[PermissionRepository]("unittest")
          permission <- repo.get.addPermission(Permission(UUID.randomUUID(), "test permission", 1))
          _ <- repo.get.deletePermission(permission.id)
          allPermissions <- repo.get.getPermissions
        } yield assertTrue(!allPermissions.exists(p => p.id == permission.id))
      },
      test(label = "it should get permissions by id") {
        for {
          repo <- ZIO.serviceAt[PermissionRepository]("unittest")
          testState <- ZIO.service[ZState[TestScenario]]
          testData <- testState.get
          permissions <- repo.get.getPermissionsByIds(testData.permissions.map(p => p.id))
        } yield assertTrue(permissions == testData.permissions)
      },
      test("it should delete roles from the database") {
        for {
          repo <- ZIO.serviceAt[RoleRepository]("unittest")
          testState <- ZIO.service[ZState[TestScenario]]
          testData <- testState.get
          role <- repo.get.addRole(Role(RoleId(UUID.randomUUID()), "test role", testData.permissions))
          _ <- repo.get.deleteRole(RoleId.unwrap(role.id))
          allRoles <- repo.get.getRoles
        } yield assertTrue(!allRoles.exists(r => r.id == role.id))
      },
      test(label = "it should delete organization") {
        for {
          repo <- ZIO.serviceAt[UserRepository]("unittest")
          org <- repo.get.addOrganization(Organization(UUID.randomUUID(), "test org2"))
          _ <- repo.get.deleteOrganization(org.id)
          allOrgs <- repo.get.getOrganizations
        } yield assertTrue(!allOrgs.exists(o => o.id == org.id))
      },
      test("password auth should fail with wrong password") {
        for {
          repo <- ZIO.serviceAt[UserRepository]("unittest")
          testState <- ZIO.service[ZState[TestScenario]]
          testData <- testState.get
          result <- repo.get.checkUserPassword(testData.users.head.logins.head.userName, "wrong password").flip
        } yield assertTrue(result == GeneralError.IncorrectPassword)
      }
    )
      + suite("fetch users")(fetchUsers))
      .provideShared(
        ZState.initial(TestScenario.create),
        DataSourceLayer.sqlite(envKeys),
        DataSourceLayer.quill(envKeys),
        UserRepositoryLive.sqliteLayer(envKeys) ++ RoleRepositoryLive.sqliteLayer(envKeys) ++ PermissionRepositoryLive
          .layer(envKeys),
        Argon2.passwordFactory
      ) @@ sequential @@ samples(1) @@ nondeterministic @@ beforeAll {
      DbManagement.provisionDatabase(envKeys(0)).provide(DbManagementLive.live)
    }
  }
