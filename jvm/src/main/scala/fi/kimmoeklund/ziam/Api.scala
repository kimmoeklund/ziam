package fi.kimmoeklund.ziam

import fi.kimmoeklund.domain.FormError.InputValueInvalid
import fi.kimmoeklund.service.UserRepository
import zio.*
import zio.http.*
import zio.json.*
import zio.metrics.*

import scala.util.Try
import fi.kimmoeklund.service.DataSourceLayer
import fi.kimmoeklund.service.UserRepositoryLive

object ZiamApi:
  def apply() = Http.collectZIO[Request] {

    case request @ Method.POST -> Root / "api" / db / "auth" =>
      val effect = for {
        repo <- ZIO.serviceAt[UserRepository](db)
        form <- request.body.asURLEncodedForm.orElseFail(InputValueInvalid("body", "unable to parse as form"))
        userName <- ZIO.fromTry(Try(form.get("username").get.stringValue.get))
        password <- ZIO.fromTry(Try(form.get("password").get.stringValue.get))
        user <- repo.get.checkUserPassword(userName, password)
      } yield user
      effect.foldZIO(
        _ => ZIO.succeed(Response.status(Status.Unauthorized)),
        user => ZIO.succeed(Response.json(user.toJson))
      )

    case Method.GET -> Root / "api" / "auth" =>
      ZIO.succeed(Response.status(Status.Unauthorized))

  }
