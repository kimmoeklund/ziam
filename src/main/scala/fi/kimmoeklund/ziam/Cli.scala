//package fi.kimmoeklund.ziam
//
//import fi.kimmoeklund.domain.{NewPasswordCredentials, NewPasswordUser}
//import fi.kimmoeklund.service.*
//import zio.Console.printLine
//import zio.*
//import zio.cli.HelpDoc.Span.text
//import zio.cli.*
//
//import java.util.UUID
//import fi.kimmoeklund.domain.UserId
//
//object Cli extends ZIOCliDefault {
//
//  case class UserOptions(database: String, username: String, password: String)
//
//  val options =
//    (Options.text("database") ++ Options.text("username") ++ Options.text("password"))
//      .as(UserOptions(_, _, _))
//  val help: HelpDoc =
//    HelpDoc.p(
//      "Initializes a new database with an admin user"
//    )
//
//  val command =
//    Command("ziam").subcommands(Command("create", options)).withHelp(help)
//
//  val cliApp = CliApp.make(
//    name = "Ziam Cli",
//    version = "0.0.1",
//    summary = text("Ziam CLI"),
//    command = command
//  ) { case UserOptions(database, username, password) =>
//    (for {
//      repo <- ZIO.serviceAt[UserRepository](database)
//      db <- DbManagement.provisionDatabase(database)
//      user <- repo.get.addUser(
//        NewPasswordUser(
//          UserId(UUID.randomUUID()),
//          "database admin",
//          NewPasswordCredentials(username, password),
//          Set()
//        )
//      )
//    } yield ()).provide(
//      DataSourceLayer.sqlite(Vector(database)),
//      DataSourceLayer.quill(Vector(database)),
//      Argon2.passwordFactory,
//      UserRepositoryLive.environment(Vector(database)),
//      DbManagementLive.live
//    )
//  }
//}
