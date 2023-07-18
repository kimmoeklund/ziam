package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.{NewPasswordCredentials, NewPasswordUser, PasswordCredentials, User}
import zio.*
import zio.http.{html as _, *}
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import fi.kimmoeklund.html.{Effects, Renderer, SimplePage, SiteMap}
import fi.kimmoeklund.service.UserRepository

import java.util.UUID

object UsersEffects extends Effects[UserRepository, User] with Renderer[User]:

  extension (u: User) {

    def htmlTableRow: Dom = tr(
      PartialAttribute("hx-target") := "this",
      PartialAttribute("hx-swap") := "delete",
      td(u.id.toString),
      td(u.name),
      td(u.logins.map(_.userName).mkString(",")),
      td(u.organization.name),
      td(u.roles.map(_.name).mkString(", ")),
      td(
        button(
          classAttr := "btn btn-danger" :: Nil,
          "Delete",
          PartialAttribute("hx-delete") := "/users/" + u.id.toString
        )
      )
    )

    def usersTableSwap: Dom =
      tBody(
        PartialAttribute("hx-swap-oob") := "beforeend:#users-table",
        htmlTableRow
      )
  }

  override def htmlTable(args: List[User]): Html =
    table(
      classAttr := "table" :: Nil,
      tHead(
        tr(
          th("Id"),
          th("Name"),
          th("User login"),
          th("Organization"),
          th("Roles")
        )
      ),
      tBody(id := "users-table", args.map(htmlTableRow))
    ) ++ form(
      idAttr := "add-users-form",
      PartialAttribute("hx-post") := "/users",
      PartialAttribute("hx-swap") := "none",
      label(
        "Name",
        forAttr := "name-field",
        classAttr := "form-label" :: Nil
      ),
      input(idAttr := "name-field", nameAttr := "name", classAttr := "form-control" :: Nil, typeAttr := "text"),
      label(
        "Username",
        classAttr := "form-label" :: Nil,
        forAttr := "username-field"),
      input(
        id := "username-field",
      nameAttr := "username",
      classAttr := "form-control" :: Nil,
      typeAttr := "text"),
      label(
        "Password",
        classAttr := "form-label" :: Nil,
        forAttr := "password-field",
      ),
      input(
        id := "password-field",
        nameAttr := "password",
        classAttr := "form-control" :: Nil,
        typeAttr := "password"
      ),
      input(
        id := "password-confirmation",
        nameAttr := "password-confirmation",
        classAttr := "form-control" :: Nil,
        typeAttr := "password"
      ),
      select(idAttr := "organization-select", classAttr := "form-select" :: Nil,
        nameAttr := "organization",
        PartialAttribute("hx-get") := "/organizations/options",
        PartialAttribute("hx-trigger") := "revealed",
        PartialAttribute("hx-params") := "none",
        PartialAttribute("hx-target") := "#organization-select",
        PartialAttribute("hx-swap") := "innerHTML"),
      button(typeAttr := "submit", classAttr := "btn" :: "btn-primary" :: Nil, "Add")
    )

  override def postResult(item: User): Html = item.usersTableSwap

  override def postEffect(req: Request): ZIO[UserRepository, Option[Nothing] | Throwable, User] =
    val userId = UUID.randomUUID()
    for {
      form <- req.body.asURLEncodedForm
      name <- ZIO.fromOption(form.get("name").get.stringValue)
      username <- ZIO.fromOption(form.get("username").get.stringValue)
      password <- ZIO.fromOption(form.get("password").get.stringValue)
      passwordConfirmation <- ZIO.fromOption(form.get("password-confirmation").get.stringValue)
      orgIdString <- ZIO.fromOption(form.get("organization").get.stringValue)
      orgUUid <- ZIO.attempt(UUID.fromString(orgIdString))
      orgOpt <- UserRepository.getOrganizationById(orgUUid)
      organization <- ZIO.fromOption(orgOpt)
      passwordCredentials <- ZIO.fromOption(NewPasswordCredentials(userId, username, password, passwordConfirmation))
      user <- UserRepository.addUser(NewPasswordUser(userId, name, organization, passwordCredentials, List()))
  } yield (user)

  override def deleteEffect(id: String): ZIO[UserRepository, Option[Nothing] | Throwable, Unit] = for {
    uuid <- ZIO.attempt(UUID.fromString(id))
    _ <- UserRepository.deleteUser(uuid)
  } yield ()

  override def optionsList(args: List[User]): Html = ???

  def getEffect = for {
    users <- UserRepository.getUsers()
  } yield users

object UsersPage extends SimplePage(Root / "users", SiteMap.tabs.setActiveTab(SiteMap.usersTab), UsersEffects)
