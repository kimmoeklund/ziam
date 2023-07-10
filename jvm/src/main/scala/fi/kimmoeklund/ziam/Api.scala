package fi.kimmoeklund.ziam

import zio._
import zio.http._
import zio.metrics._
import fi.kimmoeklund.service.UserRepository
import zio.json._

object ZiamApi:
  def apply(): HttpApp[UserRepository, Nothing] = Http.collectZIO[Request] {
    case Method.GET -> Root / "users" => 
      val effect = for {
        users <- UserRepository.getUsers()
      } yield users
      effect.foldZIO(_ => ZIO.succeed(Response.status(Status.BadRequest)), users => ZIO.succeed(Response.json(users.toJson)))      
  }

