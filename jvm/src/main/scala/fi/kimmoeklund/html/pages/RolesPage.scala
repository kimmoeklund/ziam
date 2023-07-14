package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.Role
import zio.*
import zio.http.{html as _, *}
import zio.http.html.{th, *}
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
    r <- UserRepository.addRole(Role(UUID.randomUUID(), name, Seq()))
  } yield r

  override def deleteEffect(id: String) = for {
    uuid <- ZIO.fromOption(try {
      Some(UUID.fromString(id))
    } catch {
      case e: IllegalArgumentException => None
    })
    _ <- UserRepository.deletePermission(uuid)
  } yield ()

  override def postItemRenderer(item: Role): Html = item.htmlTableRowSwap

  override def listRenderer(permissions: List[Role]): Html = {
    table(
      classAttr := "table" :: Nil,
      tHead(
        tr(
          th("Target"),
          th("Permission")
        )
      ),
      tBody(id := "permissions-table", permissions.map(htmlTableRow))
    ) ++
      form(
        idAttr := "add-permission",
        PartialAttribute("hx-post") := "/permissions",
        PartialAttribute("hx-swap") := "none",
        label(
          "Target",
          forAttr := "target",
          input(idAttr := "target", nameAttr := "target", classAttr := "form-control" :: Nil, typeAttr := "text")
        ),
        label(
          "Permission",
          forAttr := "permission",
          input(
            id := "permission",
            nameAttr := "permission",
            classAttr := "form-control" :: Nil,
            typeAttr := "text"
          )
        ),
        button(typeAttr := "submit", classAttr := "btn" :: "btn-primary" :: Nil, "Add")
      ) ++
      script(srcAttr := "/scripts")
  }
}

object RolesPage extends SimplePage(Root / "roles", SiteMap.tabs.setActiveTab(SiteMap.rolesTab), RolesEffects)
