package fi.kimmoeklund.html.pages
import fi.kimmoeklund.domain.FormError.{Missing, ValueInvalid}
import fi.kimmoeklund.domain.{ErrorCode, User}
import fi.kimmoeklund.html.{zioFromField}
import fi.kimmoeklund.repository.{UserRepositoryLive, QuillCtx}
import zio.ZIO
import zio.http.*
import zio.http.template.*
import zio.prelude.Newtype

import java.time.Duration
import scala.util.Try
import fi.kimmoeklund.templates.html.login_page
import fi.kimmoeklund.domain.Identifiable

object CookieSecret extends Newtype[String]
type CookieSecret = CookieSecret.Type

trait LoginPage[A]:
  val loginPath: Path 
  val logoutPath: Path 
  val cookieSecret: CookieSecret
  def loginCookie(user: A): Cookie.Response
  def logoutCookie: Cookie.Response
  def doLogin(using quill: QuillCtx)(request: Request): ZIO[UserRepositoryLive, ErrorCode, A]
  def showLogin: String
end LoginPage

case class DefaultLoginPage(
    loginPath: Path,
    logoutPath: Path,
    db: String,
    cookieSecret: CookieSecret,
) extends LoginPage[User] {

  override def showLogin: String = login_page().body
  override def doLogin(using QuillCtx)(request: Request) =
    for {
      repo     <- ZIO.service[UserRepositoryLive]
      form     <- request.body.asURLEncodedForm.orElseFail(ValueInvalid("body", "unable to parse as form"))
      userName <- form.zioFromField("username")
      password <- form.zioFromField("password")
      user     <- repo.checkUserPassword(userName, password)
    } yield (user)
  override def loginCookie(user: User) =
    Cookie.Response(db, user.id.toString, None, Some(Path(db)), false, true).sign(CookieSecret.unwrap(cookieSecret))
  override def logoutCookie = Cookie
    .Response(db, "", None, Some(Path(db)), false, true, Some(Duration.ZERO))
    .sign(CookieSecret.unwrap(cookieSecret))
}
