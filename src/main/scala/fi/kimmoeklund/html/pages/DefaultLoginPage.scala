package fi.kimmoeklund.html.pages
import fi.kimmoeklund.domain.FormError.{Missing, ValueInvalid}
import fi.kimmoeklund.domain.{ErrorCode, User}
import fi.kimmoeklund.html.{htmxHead, zioFromField}
import fi.kimmoeklund.repository.{UserRepositoryLive, QuillCtx}
import zio.ZIO
import zio.http.*
import zio.http.template.*
import zio.prelude.Newtype

import java.time.Duration
import scala.util.Try

object CookieSecret extends Newtype[String]
type CookieSecret = CookieSecret.Type

trait LoginPage[A]:
  val loginPath: String
  val logoutPath: String
  val cookieSecret: CookieSecret
  val loginCookie: Cookie.Response
  val logoutCookie: Cookie.Response
  def doLogin(using quill: QuillCtx)(request: Request): ZIO[UserRepositoryLive, ErrorCode, A]
  def showLogin: Html
end LoginPage

final case class DefaultLoginPage private (
    loginPath: String,
    logoutPath: String,
    db: String,
    cookieSecret: CookieSecret,
    loginCookie: Cookie.Response,
    logoutCookie: Cookie.Response
) extends LoginPage[User] {

  override def showLogin: Html =
    html(
      htmxHead ++ body(
        div(
          classAttr := "container",
          form(
            idAttr     := "login-form",
            actionAttr := s"/$db/$loginPath",
            methodAttr := "post",
            label("Username", classAttr := "form-label", forAttr := "username-field"),
            input(
              id        := "username-field",
              nameAttr  := "username",
              classAttr := "form-control",
              typeAttr  := "text"
            ),
            label(
              "Password",
              classAttr := "form-label",
              forAttr   := "password-field"
            ),
            input(
              id        := "password-field",
              nameAttr  := "password",
              classAttr := "form-control",
              typeAttr  := "password"
            ),
            button(typeAttr := "submit", classAttr := "btn btn-primary", "Login")
          )
        )
      )
    )

  override def doLogin(using QuillCtx)(request: Request) =
    for {
      repo     <- ZIO.service[UserRepositoryLive]
      form     <- request.body.asURLEncodedForm.orElseFail(ValueInvalid("body", "unable to parse as form"))
      userName <- form.zioFromField("username")
      password <- form.zioFromField("password")
      user     <- repo.checkUserPassword(userName, password)
    } yield (user)
}

object DefaultLoginPage:
  def apply(loginPath: String, logoutPath: String, db: String, cookieSecret: CookieSecret) =
    val loginCookie =
      Cookie.Response(db, "", None, Some(Path(db)), false, true).sign(CookieSecret.unwrap(cookieSecret))
    val logoutCookie = Cookie
      .Response(db, "", None, Some(Path(db)), false, true, Some(Duration.ZERO))
      .sign(CookieSecret.unwrap(cookieSecret))
    new DefaultLoginPage(loginPath, logoutPath, db, cookieSecret, loginCookie, logoutCookie)
