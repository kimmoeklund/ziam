package fi.kimmoeklund.infra

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

import zio._

object PostgresContainer:
  def make(imageName: String = "postgres:alpine") =
    ZIO.acquireRelease {
      ZIO.attempt {
        val c = new PostgreSQLContainer(
          dockerImageNameOverride = Option(imageName).map(DockerImageName.parse)
        )
          .configure { a =>
            a.withUsername("ziam")
            a.withDatabaseName("ziam")
            a.withPassword("ziam")
            a.withInitScript("ziam_schema.sql")
           // a.withReuse(true)
            ()
          }      
        c.start()
        c
      }
  } { container => ZIO.succeed(()) } //ZIO.attempt(container.stop()).orDie }
