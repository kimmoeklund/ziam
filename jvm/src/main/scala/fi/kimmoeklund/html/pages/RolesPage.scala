package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.Role
import zio.*
import zio.http.{html as _, *}
import zio.http.html.{option, th, *}
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import fi.kimmoeklund.html.{Effects, Renderer, SimplePage, SiteMap}

import java.util.UUID
import fi.kimmoeklund.service.UserRepository

object RolesEffects extends Effects[UserRepository, Role] with Renderer[Role] {

  extension (r: Role) {
    def htmlTableRow: Dom = tr(
      PartialAttribute("hx-target") := "this",
      PartialAttribute("hx-swap") := "delete",
      td(r.name),
      td(r.id.toString),
      td(r.permissions.map(p => s"${p.target} ${p.permission}").mkString(", ")),
      td(
        button(
          classAttr := "btn btn-danger" :: Nil,
          "Delete",
          PartialAttribute("hx-delete") := "/roles/" + r.id.toString
        )
      )
    )

    def htmlTableRowSwap: Dom =
      tBody(
        PartialAttribute("hx-swap-oob") := "beforeend:#roles-table",
        htmlTableRow
      )
  }

  override def getEffect = for {
    roles <- UserRepository.getRoles()
   } yield roles

  def postEffect(request: Request) = for {
    form <- request.body.asURLEncodedForm
    name <- ZIO.fromOption(form.get("name").get.stringValue)
    inputUuids <- ZIO.fromOption(form.get("permissions").get.stringValue.map(s => s.split(",").map(uuidStr =>
      try {
        Some(UUID.fromString(uuidStr))
      } catch {
        case e: IllegalArgumentException => None
      })))
    permissions <- UserRepository.getPermissionsById(inputUuids.flatten.toList)
    _ <- ZIO.logInfo(inputUuids.mkString(","))
    r <- UserRepository.addRole(Role(UUID.randomUUID(), name, permissions))
  } yield r

  override def deleteEffect(id: String) = for {
    uuid <- ZIO.fromOption(try {
      Some(UUID.fromString(id))
    } catch {
      case e: IllegalArgumentException => None
    })
    _ <- UserRepository.deleteRole(uuid)
  } yield ()

  override def postResult(item: Role): Html = item.htmlTableRowSwap

  override def htmlTable(roles: List[Role]): Html = {
    table(
      classAttr := "table" :: Nil,
      tHead(
        tr(
          th("Role"),
          th("ID"),
          th("Permissions")
        )
      ),
      tBody(id := "roles-table", roles.map(htmlTableRow))
    ) ++
      form(
        idAttr := "add-role",
        PartialAttribute("hx-post") := "/roles",
        PartialAttribute("hx-swap") := "none",
        div(classAttr := "mb-3" :: Nil,
          label(
            "Role name",
            forAttr := "name-field",
            classAttr := "form-label" :: Nil,
          ),
          input(idAttr := "name-field", nameAttr := "name", classAttr := "form-control" :: Nil, typeAttr := "text"),
        ),
        div(classAttr := "mb-3" :: Nil,
          label(
            "Permissions",
            classAttr := "form-label" :: Nil,
            forAttr := "permissions-select"),
          select(idAttr := "permissions-select", classAttr := "form-select" :: Nil, multipleAttr := "multiple",
            nameAttr := "permissions",
            PartialAttribute("hx-params") := "none",
            PartialAttribute("hx-get") := "/permissions/options",
            PartialAttribute("hx-trigger") := "revealed",
            PartialAttribute("hx-target") := "#permissions-select",
            PartialAttribute("hx-swap") := "innerHTML"),
        ),
        button(typeAttr := "submit", classAttr := "btn" :: "btn-primary" :: Nil, "Add")
      ) ++
      script(srcAttr := "/scripts")
  }

  override def optionsList(args: List[Role]): Html =
    args.map(r => option(r.name, valueAttr := r.id.toString))
}

object RolesPage extends SimplePage(Root / "roles", SiteMap.tabs.setActiveTab(SiteMap.rolesTab), RolesEffects)
