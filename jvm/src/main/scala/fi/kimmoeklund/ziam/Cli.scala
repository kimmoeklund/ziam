package fi.kimmoeklund.ziam

import zio.Console.printLine
import zio.cli.*
import zio.cli.HelpDoc.Span.text

object ZiamCli extends ZIOCliDefault {

//  val options = Options.text("username") ++ Options.text("password")
  val arguments: Args[String] =
    Args.text("organization") // ++ Args.text("username") ++ Args.text("password")
  val help: HelpDoc = HelpDoc.p("Creates a copy of an existing repository")

  val command: Command[(String)] =
    Command("init", arguments).withHelp(help)

  // Define val cliApp using CliApp.make
  val cliApp = CliApp.make(
    name = "Ziam Cli",
    version = "0.0.1",
    summary = text("Ziam CLI"),
    command = command
  ) {
    // Implement logic of CliApp
    case _ => printLine("executing git clone")
  }

}
