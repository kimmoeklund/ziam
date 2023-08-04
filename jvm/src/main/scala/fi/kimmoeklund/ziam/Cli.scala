package fi.kimmoeklund.ziam

import fi.kimmoeklund.domain.{NewPasswordCredentials, NewPasswordUser, Organization}
import fi.kimmoeklund.service.*
import zio.*
import zio.Console.printLine
import zio.cli.*
import zio.cli.HelpDoc.Span.text

import java.util.UUID

object Cli extends ZIOCliDefault {

  case class UserOptions(name: String, username: String, password: String)

  val options =
    (Options.text("name") ++ Options.text("username") ++ Options.text("password"))
      .as(UserOptions(_, _, _))
  val arguments: Args[String] =
    Args.text("organization")
  val help: HelpDoc =
    HelpDoc.p(
      "Initializes a new database, creates organization for it assigns the given user as the admin to the organization"
    )

  val command =
    Command("create").subcommands(Command("organization", options)).withHelp(help)

  val cliApp = CliApp.make(
    name = "Ziam Cli",
    version = "0.0.1",
    summary = text("Ziam CLI"),
    command = command
  ) { case UserOptions(name, username, password) =>
    (for {
      _ <- printLine(s"Creating organization $name")
      _ <- printLine(s"current dir: ${new java.io.File(".").getCanonicalPath}")
      repo <- ZIO.serviceAt[UserRepository]("ziam")
      db <- DbManagement.provisionDatabase(name)
      org <- repo.get.addOrganization(Organization(UUID.randomUUID(), name))
      _ <- printLine(s"Created organization $org")
      user <- repo.get.addUser(
        NewPasswordUser(
          UUID.randomUUID(),
          "Organization admin",
          org,
          NewPasswordCredentials(username, password),
          Seq()
        )
      )
    } yield ()).provide(
      DataSourceLayer.sqlite(Vector(name)),
      DataSourceLayer.quill(Vector(name)),
      Argon2.passwordFactory,
      UserRepositoryLive.sqliteLayer(Vector(name)),
      DbManagementLive.live
    )
  }
}
