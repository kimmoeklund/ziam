package fi.kimmoeklund.html.pages

import fi.kimmoeklund.service.UserRepository
import fi.kimmoeklund.domain.FormError.InputValueInvalid
import fi.kimmoeklund.html.Page
import fi.kimmoeklund.domain.ErrorCode
import zio.*
import zio.http.html.*
import zio.http.html.Html.fromDomElement
import zio.http.{html as _, *}
import scala.util.Try
import zio.http.html.Attributes.PartialAttribute
import fi.kimmoeklund.html.htmxHead

final case class LoginPage(loginPath: String, logoutPath: String, db: String, authCookie: Cookie.Response, logoutCookie: Cookie.Response) {
  def showLogin: Html = 
    html(htmxHead ++ body(div(classAttr := "container" :: Nil, 
    form(
      idAttr := "login-form",
      actionAttr := s"/$db/$loginPath",
      methodAttr := "post",
      label("Username", classAttr := "form-label" :: Nil, forAttr := "username-field"),
      input(id := "username-field", nameAttr := "username", classAttr := "form-control" :: Nil, typeAttr := "text"),
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
      button(typeAttr := "submit", classAttr := "btn" :: "btn-primary" :: Nil, "Login")))))

  def loginApp: App[Map[String, UserRepository]] = Http.collectZIO[Request] {
    case Method.GET -> Root / this.db / this.loginPath => ZIO.succeed(Response.html(showLogin))
    case request @ Method.POST -> Root / this.db / this.loginPath =>
      (for {
        repo <- ZIO.serviceAt[UserRepository](this.db)
        form <- request.body.asURLEncodedForm.orElseFail(InputValueInvalid("body", "unable to parse as form"))
        userName <- ZIO.fromTry(Try(form.get("username").get.stringValue.get))
        password <- ZIO.fromTry(Try(form.get("password").get.stringValue.get))
        user <- repo.get.checkUserPassword(userName, password)
      } yield (user)).foldZIO(
        _ => ZIO.succeed(Response.html(showLogin)),
        _ => ZIO.succeed(Response.seeOther(URL(Root / db / "users")).addCookie(authCookie))
      )
    case Method.GET -> Root / db / this.logoutPath => ZIO.succeed(Response.html(showLogin).addCookie(logoutCookie))
  }
}


