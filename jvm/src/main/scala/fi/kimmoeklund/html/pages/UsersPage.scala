package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.User
import zio.*
import zio.http.{html as _, *}
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import fi.kimmoeklund.html.{Effects, Renderer, SimplePage, SiteMap}
import fi.kimmoeklund.service.UserRepository

object UsersEffects extends Effects[UserRepository, User] with Renderer[User]:

  extension (u: User) {

    def htmlTableRow: Dom = tr(
      PartialAttribute("hx-target") := "this",
      PartialAttribute("hx-swap") := "delete",
      td(u.id.toString),
      td(u.name),
      td(u.logins.map(_.userName).mkString(",")),
      td(u.organization.name),
      td(u.roles.mkString(", ")),
      td(
        button(
          classAttr := "btn btn-danger" :: Nil,
          "Delete",
          PartialAttribute("hx-delete") := "/users/" + u.id.toString
        )
      )
    )

    def usersTableSwap2: Dom =
      tBody(
        PartialAttribute("hx-swap-oob") := "beforeend:#users-table",
        htmlTableRow
      )
  }

  override def listRenderer(args: List[User]): Html =
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
        forAttr := "Name",
        input(idAttr := "name", nameAttr := "Name", classAttr := "form-control" :: Nil, typeAttr := "text")
      ),
      label(
        "Username",
        forAttr := "username",
        input(
          id := "username",
          nameAttr := "username",
          classAttr := "form-control" :: Nil,
          typeAttr := "text"
        )
      ),
      label(
        "Password",
        forAttr := "password",
        input(
          id := "password",
          nameAttr := "password",
          classAttr := "form-control" :: Nil,
          typeAttr := "password"
        ),
        input(
          id := "password_confirmation",
          nameAttr := "password_confirmation",
          classAttr := "form-control" :: Nil,
          typeAttr := "password"
        ),
        input(
          nameAttr := "organization",
          classAttr := "form-control" :: Nil,
          typeAttr := "hidden",
          valueAttr := "37a6f38f-1d7c-4553-a2a0-6481ab5b8c8d"
        )
      ),
      button(typeAttr := "submit", classAttr := "btn" :: "btn-primary" :: Nil, "Add")
    )

  override def postItemRenderer(item: User): Html = ???

  override def postEffect(req: Request): ZIO[UserRepository, Option[Nothing] | Throwable, User] = ???

  override def deleteEffect(id: String): ZIO[UserRepository, Option[Nothing] | Throwable, Unit] = ???

  def getEffect = for {
    users <- UserRepository.getUsers()
  } yield users

object UsersPage extends SimplePage(Root / "users", SiteMap.tabs.setActiveTab(SiteMap.usersTab), UsersEffects)
