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

    case request @ Method.POST -> Root / "api" / "auth" =>
      val effect = for {
        repo <- ZIO.serviceAt[UserRepository]("ziam")
        form <- request.body.asURLEncodedForm.orElseFail(InputValueInvalid("body", "unable to parse as form"))
        userName <- ZIO.fromTry(Try(form.get("username").get.stringValue.get))
        password <- ZIO.fromTry(Try(form.get("password").get.stringValue.get))
        user <- repo.get.checkUserPassword(userName, password)
      } yield user
      effect.foldZIO(
        _ => ZIO.succeed(Response.status(Status.Unauthorized)),
        user => ZIO.succeed(Response.json(user.toJson))
      )

//TODO API for adding site
//    case request @ Method.POST -> Root / "api" / "sites" =>
//      ZIO.environment.flatMap { env =>
//        {
//          env ++ DataSourceLayer.sqlite("newsite")
//          env ++ DataSourceLayer.quill("newsite")
////          env.add(UserRepositoryLive.sqliteLayer("newsite"))
//        }
//      }
//
    case Method.GET -> Root / "api" / "auth" =>
      ZIO.succeed(Response.status(Status.Unauthorized))

  }
