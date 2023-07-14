package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.User
import zio.*
import zio.http.{html as _, *}
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import fi.kimmoeklund.html.{Effects, Renderer, SimplePage, SiteMap}
import fi.kimmoeklund.service.UserRepository

extension (u: User) {

  def htmlTableRow: Dom = tr(
    PartialAttribute("hx-target") := "this",
    PartialAttribute("hx-swap") := "delete",
    td(u.id.toString),
    td(u.name),
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

  def usersTableSwap: Dom =
    tBody(
      PartialAttribute("hx-swap-oob") := "beforeend:#users-table",
      htmlTableRow
    )
}

object UsersEffects extends Effects[UserRepository, User] with Renderer[User]:

  override def listRenderer(args: List[User]): Html =
    table(
      classAttr := "table" :: Nil,
      tHead(
        tr(
          th("Id"),
          th("Name"),
          th("Organization"),
          th("Roles"),
        )
      ),
      tBody(id := "users-table", args.map(htmlTableRow))
    )

  override def postItemRenderer(item: User): Html = ???

  override def postEffect(req: Request): ZIO[UserRepository, Option[Nothing] | Throwable, User] = ???

  override def deleteEffect(id: String): ZIO[UserRepository, Option[Nothing] | Throwable, Unit] = ???

  def getEffect = for {
    users <- UserRepository.getUsers()
  } yield users

object UsersPage extends SimplePage(Root / "users", SiteMap.tabs.setActiveTab(SiteMap.usersTab), UsersEffects)
