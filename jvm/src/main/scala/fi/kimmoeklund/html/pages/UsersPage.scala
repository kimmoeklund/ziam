package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.*
import fi.kimmoeklund.domain.FormError.InputValueInvalid
import fi.kimmoeklund.html.forms.*
import fi.kimmoeklund.html.{Effects, Renderer}
import fi.kimmoeklund.service.UserRepository
import zio.*
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.{html as _, *}

import java.util.UUID

object UsersEffects extends Effects[UserRepository, User] with Renderer[User]:

  extension (u: User) {

    def htmlTableRow: Dom = tr(
      PartialAttribute("hx-target") := "this",
      PartialAttribute("hx-swap") := "delete",
      td(u.id.toString),
      td(u.name),
      td(u.logins.map(l => s"${l.userName} (${l.loginType.toString})").mkString(",")),
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
      label("Username", classAttr := "form-label" :: Nil, forAttr := "username-field"),
      input(id := "username-field", nameAttr := "username", classAttr := "form-control" :: Nil, typeAttr := "text"),
      label("Organization", classAttr := "form-label" :: Nil, forAttr := "organization-select"),
      select(
        idAttr := "organization-select",
        classAttr := "form-select" :: Nil,
        nameAttr := "organization",
        PartialAttribute("hx-get") := "/organizations/options",
        PartialAttribute("hx-trigger") := "revealed",
        PartialAttribute("hx-params") := "none",
        PartialAttribute("hx-target") := "#organization-select",
        PartialAttribute("hx-swap") := "innerHTML"
      ),
      label("Roles", classAttr := "form-label" :: Nil, forAttr := "roles-select"),
      select(
        idAttr := "roles-select",
        multipleAttr := "multiple",
        classAttr := "form-select" :: Nil,
        nameAttr := "roles",
        PartialAttribute("hx-get") := "/roles/options",
        PartialAttribute("hx-trigger") := "revealed",
        PartialAttribute("hx-params") := "none",
        PartialAttribute("hx-target") := "#roles-select",
        PartialAttribute("hx-swap") := "innerHTML"
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
      input(
        id := "password-confirmation",
        nameAttr := "password-confirmation",
        classAttr := "form-control" :: Nil,
        typeAttr := "password"
      ),
      button(typeAttr := "submit", classAttr := "btn" :: "btn-primary" :: Nil, "Add") ++
        script(srcAttr := "/scripts")
    )

  override def postResult(item: User): Html = item.usersTableSwap

  override def postEffect(req: Request) = {
    val userId = UUID.randomUUID()
    for {
      repo <- ZIO.serviceAt[UserRepository]("ziam")
      form <- req.body.asURLEncodedForm.orElseFail(InputValueInvalid("body", "unable to parse as form"))
      newUserForm <- NewUserForm
        .fromOptions(
          form.get("name"),
          form.get("organization"),
          form.get("roles"),
          form.get("username"),
          form.get("password"),
          form.get("password-confirmation")
        )
        .toZIO
      orgAndRoles <- repo.get
        .getOrganizationById(newUserForm.organizationId)
        .validatePar(repo.get.getRolesByIds(newUserForm.roleIds.toSeq))
      user <- repo.get.addUser(
        NewPasswordUser(userId, newUserForm.name, orgAndRoles._1, newUserForm.credentials, orgAndRoles._2)
      )
    } yield (user)
  }

  override def deleteEffect(id: String) = for {
    uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(InputValueInvalid("id", "unable to parse as UUID"))
    repo <- ZIO.serviceAt[UserRepository]("ziam")
    _ <- repo.get.deleteUser(uuid)
  } yield ()

  override def optionsList(args: List[User]): Html = ???

  def getEffect = for {
    _ <- ZIO.logInfo("getting users")
    repo <- ZIO.serviceAt[UserRepository]("ziam")
    _ <- ZIO.logInfo("getting users22")
    users <- repo.get.getUsers
  } yield users
