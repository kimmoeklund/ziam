package fi.kimmoeklund.html.pages
import fi.kimmoeklund.domain.FormError.{InputValueInvalid, MissingInput}
import fi.kimmoeklund.domain.{ErrorCode, User}
import fi.kimmoeklund.html.{LoginPage, htmxHead}
import fi.kimmoeklund.service.UserRepository
import zio.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.html.*
import zio.http.{html as _, *}

import scala.util.Try

final case class DefaultLoginPage(loginPath: String, logoutPath: String, db: String)
    extends LoginPage[UserRepository, User] {
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
    val effect = for {
      repo <- ZIO.serviceAt[UserRepository](this.db)
      form <- request.body.asURLEncodedForm.orElseFail(InputValueInvalid("body", "unable to parse as form"))
      userName <- ZIO.fromTry(Try(form.get("username").get.stringValue.get)).orElseFail(MissingInput("username"))
      password <- ZIO.fromTry(Try(form.get("password").get.stringValue.get)).orElseFail(MissingInput("password"))
      user <- repo.get.checkUserPassword(userName, password)
    } yield (user)
    effect
}
