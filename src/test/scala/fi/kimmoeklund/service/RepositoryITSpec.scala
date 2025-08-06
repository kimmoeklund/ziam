package fi.kimmoeklund.service

import com.outr.scalapass.Argon2PasswordFactory
import fi.kimmoeklund.domain.*
import fi.kimmoeklund.repository.*
import zio.*
import zio.test.*
import zio.test.TestAspect.*

import java.util.UUID

final case class TestScenario(
    users: List[User],
    roles: Set[Role],
    permissions: Set[Permission]
)

object TestScenario:
  def create: TestScenario = TestScenario(List(), Set(), Set())

object UserRepositoryLiveSpec extends ZIOSpecDefault:

  val unicodeString =
    Gen.stringBounded(5, 100)(Gen.unicodeChar.filter(c => c != 0x00.toChar && !c.isControl && !c.isWhitespace))
  val asciiString = Gen.stringBounded(3, 12)(Gen.alphaNumericChar)
  val envKeys     = Vector("unittest")

  def fetchUsers = {
    val users = for {
      testData <- ZIO.serviceWithZIO[ZState[TestScenario]](_.get)
      userResults <- ZIO.collectAll(testData.users.map { user =>
        for {
          quill <- ZIO.serviceAt[QuillCtx]("unittest")
          repo  <- ZIO.service[UserRepositoryLive]
          user <- {
            given QuillCtx = quill.get
            repo.checkUserPassword(
              user.logins.head.userName,
              user.logins.head.userName
            )
          }
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
            cRole.permissions.toSeq.sorted == fRole.permissions.toSeq.sorted
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
            Permission(PermissionId.create, s"permission-$name", number)
          for {
            quill <- ZIO.serviceAt[QuillCtx]("unittest")
            repo  <- ZIO.service[PermissionRepository]
            result <- {
              given QuillCtx = quill.get
              repo.add(permission)
            }
            testData <- ZIO.service[ZState[TestScenario]]
            _        <- testData.update(data => data.copy(permissions = data.permissions + permission))
          } yield assertTrue(result == permission)
        }
      },
      test("it should add roles to the database") {
        check(asciiString) { name =>
          for {
            quill     <- ZIO.serviceAt[QuillCtx]("unittest")
            repo      <- ZIO.service[RoleRepository]
            testState <- ZIO.service[ZState[TestScenario]]
            testData  <- testState.get
            newRole <- ZIO.succeed(
              Role(RoleId(UUID.randomUUID()), s"role-$name", testData.permissions)
            )
            role <- {
              given QuillCtx = quill.get
              repo.add(newRole)
            }
            _ <- testState.update(data => data.copy(roles = data.roles + newRole))
          } yield assertTrue(role == newRole)
        }
      },
      test("it should add user to the database") {
        check(unicodeString, asciiString) { (randomName, userName) =>
          {
            val userId = UserId(UUID.randomUUID())
            for {
              quill       <- ZIO.serviceAt[QuillCtx]("unittest")
              repo        <- ZIO.service[UserRepositoryLive]
              testState   <- ZIO.service[ZState[TestScenario]]
              testData    <- testState.get
              newPassword <- NewPasswordCredentials.fromOptions(Some(userName), Some(userName), Some(userName)).toZIO
              success <- {
                given QuillCtx = quill.get
                repo.add(
                  NewPasswordUser(userId, randomName, newPassword, testData.roles)
                )
              }
              _ <- testState.update(data =>
                data.copy(
                  users = data.users :+ success
                )
              )

            } yield assertTrue(success.logins.size == 1 && success.logins.head.userName == userName)
          }
        }
      },
      test("it should remove role grants from the user") {
        for {
          quill     <- ZIO.serviceAt[QuillCtx]("unittest")
          repo      <- ZIO.service[UserRepositoryLive]
          testState <- ZIO.service[ZState[TestScenario]]
          testData  <- testState.get
          user <- {
            given QuillCtx = quill.get
            repo.update(testData.users.head.copy(roles = Set()))
          }
        } yield assertTrue(user.roles.isEmpty)
      },
      test("it should get and delete permission from the database") {
        for {
          quill <- ZIO.serviceAt[QuillCtx]("unittest")
          repo  <- ZIO.service[PermissionRepository]
          permission <- {
            given QuillCtx = quill.get
            repo.add(Permission(PermissionId.create, "test permission", 1))
          }
          allPermissions <- {
            given QuillCtx = quill.get
            repo.delete(permission.id) *> repo.getList
          }
        } yield assertTrue(!allPermissions.exists(p => p.id == permission.id))
      },
      test(label = "it should get permissions by id") {
        for {
          quill     <- ZIO.serviceAt[QuillCtx]("unittest")
          repo      <- ZIO.service[PermissionRepository]
          testState <- ZIO.service[ZState[TestScenario]]
          testData  <- testState.get
          permissions <- {
            given QuillCtx = quill.get
            repo.getByIds(testData.permissions.map(p => p.id))
          }
        } yield assertTrue(permissions == testData.permissions)
      },
      test("it should delete roles from the database") {
        for {
          quill     <- ZIO.serviceAt[QuillCtx]("unittest")
          repo      <- ZIO.service[RoleRepository]
          testState <- ZIO.service[ZState[TestScenario]]
          testData  <- testState.get
          role <- {
            given QuillCtx = quill.get
            repo.add(Role(RoleId(UUID.randomUUID()), "test role", testData.permissions))
          }
          allRoles <- {
            given QuillCtx = quill.get
            repo.delete(role.id) *> repo.getList
          }
        } yield assertTrue(!allRoles.exists(r => r.id == role.id))
      },
      test("password auth should fail with wrong password") {
        for {
          quill     <- ZIO.serviceAt[QuillCtx]("unittest")
          repo      <- ZIO.service[UserRepositoryLive]
          testState <- ZIO.service[ZState[TestScenario]]
          testData  <- testState.get
          result <- {
            given QuillCtx = quill.get
            repo.checkUserPassword(testData.users.head.logins.head.userName, "wrong password").flip
          }
        } yield assertTrue(result == GeneralError.IncorrectPassword)
      }
    )
      + suite("fetch users")(fetchUsers))
      .provideShared(
        ZState.initial(TestScenario.create),
        ZLayer.fromZIO(ZIO.service[Argon2PasswordFactory].map(UserRepositoryLive(_)))
          ++ ZLayer.succeed[RoleRepository](RoleRepositoryLive())
          ++ ZLayer.succeed[PermissionRepository](PermissionRepositoryLive()),
        ZLayer.fromZIO(ZIO.serviceWithZIO[DbManagement](_.getDatabases)),
        ZLayer.fromZIO(DataSourceLayer.sqlite),
        ZLayer.fromZIO(DataSourceLayer.quill),
        Argon2.passwordFactory,
        DbManagementLive.live
      ) @@ sequential @@ samples(1) @@ nondeterministic @@ beforeAll {
      DbManagement.provisionDatabase(envKeys.head).provide(DbManagementLive.live)
    }
  }
