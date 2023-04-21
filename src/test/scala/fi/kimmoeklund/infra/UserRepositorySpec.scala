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

object UserRepositorySpec extends ZIOSpecDefault:

  val containerLayer = ZLayer.scoped(PostgresContainer.make())
  val dataSourceLayer = ZLayer(ZIO.service[DataSourceBuilder].map(_.dataSource))
  val postgresLayer =
    Quill.Postgres.fromNamingStrategy(NamingStrategy(SnakeCase, Escape))
  val repoLayer = UserRepositoryLive.layer
  val unicodeString = Gen.stringBounded(5, 100)(Gen.unicodeChar)
  val asciiString = Gen.stringBounded(3, 12)(Gen.alphaNumericChar)

  val permissions = ListBuffer[Permission]()
  val roles = ListBuffer[Role]()
  val credentials = ListBuffer[PasswordCredentials]()
  val users = ListBuffer[User]()
  
  def makeUserFetchTest(creds: PasswordCredentials) = 
        test("it should check all users from the database") {
          for {
            _ <- Console.printLine(s"fetching user for userName ${creds.userName}")
            user <- UserRepository.checkUserPassword(
              creds.userName,
              creds.password
            )
          } yield assert(user)(equalTo(users.find(u => u.id == creds.userId)))
        }

  def secondSuite(creds: ListBuffer[PasswordCredentials]) = suite("test suite")(creds.toList.map(makeUserFetchTest): _*)

  override def spec =
    suite("user repository test with postgres test container")(
      test("it should add permission to the database") {
        check(asciiString, Gen.int) { (name, number) =>
          val permission =
            Permission(UUID.randomUUID(), s"permission-$name", number)
          permissions += permission
          for {
            result <- UserRepository.addPermission(permission)
            _ <- Console.printLine("added permission")
          } yield assert(result)(isUnit)
        }
      } @@ samples(3),
      test("it should add roles to the database") {
        check(asciiString) { name =>
          val newRole =
            Role(UUID.randomUUID(), s"role-$name", permissions.toSeq)
          roles += newRole
          for {
            role <- UserRepository.addRole(newRole)
            _ <- Console.printLine(
              s"added role with ${newRole.permissions.size} permissions"
            )
          } yield assert(role)(isUnit)
        }
      } @@ samples(3),
      test("it should add user to the database") {
        check(unicodeString, asciiString) { (randomName, userName) =>
          val userId = UUID.randomUUID()
          val newCreds = PasswordCredentials(userId, userName, userName)
          credentials += newCreds
          for {
            success <- UserRepository.addUser(
              User(userId, randomName, roles.toSeq),
              newCreds,
              Organization(UUID.randomUUID(), "test org")
            )
            _ <- Console.printLine(s"added user with ${roles.size} roles")
          } yield assert(success)(isUnit)
        }
      } @@ samples(100),
      secondSuite(credentials)      
    ).provideShared(
      containerLayer,
      DataSourceBuilderLive.layer,
      dataSourceLayer,
      postgresLayer,
      repoLayer
    ) @@ sequential
