//package fi.kimmoeklund.ziam
//
//import fi.kimmoeklund.domain.FormError.ValueInvalid
//import fi.kimmoeklund.service.{DataSourceLayer, UserRepository, UserRepositoryLive}
//import zio.*
//import zio.http.*
//import zio.json.*
//import zio.metrics.*
//
//import scala.util.Try
//
//object ZiamApi:
//  val app = Routes(
//    Method.POST / "api" / string("db") / "auth" -> handler { (db: String, request: Request) =>
//      val effect = for {
//        repo     <- ZIO.serviceAt[UserRepository](db)
//        form     <- request.body.asURLEncodedForm.orElseFail(ValueInvalid("body", "unable to parse as form"))
//        userName <- ZIO.fromTry(Try(form.get("username").get.stringValue.get))
//        password <- ZIO.fromTry(Try(form.get("password").get.stringValue.get))
//        user     <- repo.get.checkUserPassword(userName, password)
//        response <- ZIO.succeed(Response.json(user.toJson)) 
//      } yield response
//      effect.catchAll(_ => ZIO.succeed(Response.status(Status.Unauthorized)))
//      effect.foldZIO(
//        _ => ZIO.succeed(Response.status(Status.Unauthorized)),
//        user => ZIO.succeed(Response.json(user.toJson))
//      ).flatten
//    }
//  )
//    Method.GET / "api" / "auth" -> Handler.status(Status.Unauthorized)
