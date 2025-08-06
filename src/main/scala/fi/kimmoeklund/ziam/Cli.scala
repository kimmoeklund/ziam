package fi.kimmoeklund.ziam

import fi.kimmoeklund.domain.{NewPasswordCredentials, NewPasswordUser}
import fi.kimmoeklund.service.*
import zio.Console.printLine
import zio.*
import zio.cli.HelpDoc.Span.text
import zio.cli.*

import java.util.UUID
import fi.kimmoeklund.domain.UserId
import fi.kimmoeklund.repository.UserRepositoryLive
import com.outr.scalapass.Argon2PasswordFactory
import fi.kimmoeklund.repository.QuillCtx

object Cli extends ZIOCliDefault {

  case class UserOptions(database: String, username: String, password: String)

  val options =
    (Options.text("database") ++ Options.text("username") ++ Options.text("password"))
      .as(UserOptions(_, _, _))
  val help: HelpDoc =
    HelpDoc.p(
      "Initializes a new database with an admin user"
    )

  val command =
    Command("ziam").subcommands(Command("create", options)).withHelp(help)

  val cliApp = CliApp.make(
    name = "Ziam Cli",
    version = "0.0.1",
    summary = text("Ziam CLI"),
    command = command
  ) { case UserOptions(database, username, password) =>
    val dbNameLayer = ZLayer.fromZIO(ZIO.serviceWithZIO[DbManagement](_.provisionDatabase(database)))
    val dsLayer     = ZLayer.fromZIO(DataSourceLayer.sqlite.provide(ZLayer.fromZIO(ZIO.succeed(Seq(database)))))
    val quillLayer  = ZLayer.fromZIO(DataSourceLayer.quill)
    val repoLayer   = ZLayer.fromZIO(ZIO.service[Argon2PasswordFactory].map(UserRepositoryLive(_)))

    val effect = for {
      quill <- ZIO.service[Map[String, QuillCtx]]
      repo  <- ZIO.service[UserRepositoryLive]
      user <- ZIO.fromOption(
        quill
          .get(database)
          .map(quillCtx => {
            given QuillCtx = quillCtx
            repo.add(
              NewPasswordUser(
                UserId(UUID.randomUUID()),
                "database admin",
                NewPasswordCredentials(username, password),
                Set()
              )
            )
          })
      )
    } yield ()
    effect.provide(quillLayer, repoLayer, dsLayer, dbNameLayer, Argon2.passwordFactory, DbManagementLive.live)
  }
}
