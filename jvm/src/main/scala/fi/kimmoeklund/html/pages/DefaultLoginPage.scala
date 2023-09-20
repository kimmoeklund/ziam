package fi.kimmoeklund.html.pages
import fi.kimmoeklund.domain.FormError.{ValueInvalid, Missing}
import fi.kimmoeklund.domain.{ErrorCode, User}
import fi.kimmoeklund.html.htmxHead
import zio.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.html.*
import zio.http.{html as _, *}
import java.time.Duration
import zio.prelude.Newtype
import scala.util.Try
import fi.kimmoeklund.service.{ UserRepository, Repositories }

object CookieSecret extends Newtype[String]
type CookieSecret = CookieSecret.Type

trait LoginPage[A]:
  val loginPath: String
  val logoutPath: String
  val cookieSecret: CookieSecret
  val loginCookie: Cookie.Response
  val logoutCookie: Cookie.Response
  def doLogin(request: Request): ZIO[Map[String, Repositories], ErrorCode, A]
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

  def showLogin: Html =
  html(
    htmxHead ++ body(
      div(
        classAttr := "container" :: Nil,
        form(
          idAttr := "login-form",
          actionAttr := s"/$db/$loginPath",
          methodAttr := "post",
          label("Username", classAttr := "form-label" :: Nil, forAttr := "username-field"),
          input(
            id := "username-field",
            nameAttr := "username",
            classAttr := "form-control" :: Nil,
            typeAttr := "text"
          ),
          label(
            "Password",
            classAttr := "form-label" :: Nil,
            forAttr := "password-field"
          ),
          input(
            id := "password-field",
            nameAttr := "password",
            classAttr := "form-control" :: Nil,
            typeAttr := "password"
          ),
          button(typeAttr := "submit", classAttr := "btn" :: "btn-primary" :: Nil, "Login")
        )
      )
    )
  )

  def doLogin(request: Request) =
    for {
      repo <- ZIO.serviceAt[UserRepository](this.db)
      form <- request.body.asURLEncodedForm.orElseFail(ValueInvalid("body", "unable to parse as form"))
      userName <- ZIO.fromTry(Try(form.get("username").get.stringValue.get)).orElseFail(Missing("username"))
      password <- ZIO.fromTry(Try(form.get("password").get.stringValue.get)).orElseFail(Missing("password"))
      user <- repo.get.checkUserPassword(userName, password)
    } yield (user)
}

object DefaultLoginPage:
  def apply(loginPath: String, logoutPath: String, db: String, cookieSecret: CookieSecret) =
    val loginCookie =
      Cookie.Response(db, "", None, Some(Root / db), false, true).sign(CookieSecret.unwrap(cookieSecret))
    val logoutCookie = Cookie
      .Response(db, "", None, Some(Root / db), false, true, Some(Duration.ZERO))
      .sign(CookieSecret.unwrap(cookieSecret))
    new DefaultLoginPage(loginPath, logoutPath, db, cookieSecret, loginCookie, logoutCookie)
