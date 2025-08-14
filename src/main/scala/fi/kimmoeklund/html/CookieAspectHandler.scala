package fi.kimmoeklund.html

import zio.http.HandlerAspect
import fi.kimmoeklund.repository.QuillCtx
import fi.kimmoeklund.repository.UserRepositoryLive
import fi.kimmoeklund.html.pages.CookieSecret
import zio.http.Path
import zio.ZIO
import scala.util.Try
import java.util.UUID
import zio.http.Response
import zio.http.URL
import zio.http.Status
import fi.kimmoeklund.domain.UserId
import zio.http.Header.ContentLocation
import java.net.URI
import zio.http.Status
import zio.Chunk
import zio.http.Headers
import zio.http.Header

object CookieAspectHandler:
  def checkCookie(
      loginPath: Path,
      cookieSecret: CookieSecret
  ): HandlerAspect[Map[String, QuillCtx] & UserRepositoryLive, RequestContext] =
    HandlerAspect.customAuthProvidingZIO[Map[String, QuillCtx] & UserRepositoryLive, RequestContext](
      request =>
        request.path.segments.toList.match {
          case db :: site :: "page" :: tail =>
            val userEffect = for {
              cookie    <- ZIO.fromOption(request.cookie(site))
              decrypted <- ZIO.fromOption(cookie.toRequest.unSign(cookieSecret.toString))
              _         <- ZIO.log(s"userid from cookie: ${decrypted.content}")
              quills    <- ZIO.service[Map[String, QuillCtx]]
              quill     <- ZIO.fromOption(quills.get(site))
              repo      <- ZIO.service[UserRepositoryLive]
              users <- {
                given QuillCtx = quill
                val userId     = Try(UUID.fromString(decrypted.content)).toOption.map(UserId(_))
                repo.getList(userId)
              }
            } yield (users.headOption)

            userEffect.fold(
              e => {
                ZIO.logError(s"failed fetching user: ${e}")
                None
              },
              userOpt =>
                if (userOpt.isDefined)
                  Some(RequestContext(UUID.randomUUID().toString, userOpt.get))
                else
                  None
            )
          case _ =>
            ZIO.succeed(None)
        },
      Headers(Header.Location(URL(Path.root ++ loginPath))),
      Status.SeeOther
    )
