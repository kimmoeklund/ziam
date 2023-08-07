package fi.kimmoeklund.service

import com.outr.scalapass.Argon2PasswordFactory
import zio.{ZIO, ZLayer}

object Argon2 {

  val passwordFactory: ZLayer[Any, Throwable, Argon2PasswordFactory] = ZLayer.scoped {
    for {
      factory <- ZIO.attempt(Argon2PasswordFactory(parallelism = 1, memory = 19 * 1024))
    } yield (factory)
  }

}
