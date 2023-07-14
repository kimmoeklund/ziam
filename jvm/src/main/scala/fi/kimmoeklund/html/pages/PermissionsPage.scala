package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.Permission
import fi.kimmoeklund.html.Renderer
import fi.kimmoeklund.html.menu.menuHtml
import fi.kimmoeklund.html.{Effects, SiteMap, htmlSnippet, htmxHead, SimplePage}
import fi.kimmoeklund.service.UserRepository
import zio.*
import zio.http.{html as _, *}
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement

import java.util.UUID


object PermissionEffects extends Effects[UserRepository, Permission] with Renderer[Permission]:
  extension (p: Permission) {
    def htmlTableRow: Dom = tr(
      PartialAttribute("hx-target") := "this",
      PartialAttribute("hx-swap") := "delete",
      td(p.target),
      td(p.permission.toString),
      td(
        button(
          classAttr := "btn btn-danger" :: Nil,
          "Delete",
          PartialAttribute("hx-delete") := "/permissions/" + p.id.toString
        )
      )
    )

    def htmlTableRowSwap: Dom =
      tBody(
        PartialAttribute("hx-swap-oob") := "beforeend:#permissions-table",
        htmlTableRow
      )
  }

  override def getEffect = for {
    permissions <- UserRepository.getPermissions()
  } yield permissions

  def postEffect(request: Request) = for {
    form <- request.body.asURLEncodedForm
    target <- ZIO.fromOption(form.get("target").get.stringValue)
    permission <- ZIO.fromOption(form.get("permission").get.stringValue)
    permissionInt <- ZIO.fromOption {
      try {
        (Some(permission.toInt))
      } catch {
        case e: NumberFormatException => None
      }
    }
    p <- UserRepository.addPermission(Permission(UUID.randomUUID(), target, permissionInt))
  } yield p

  override def deleteEffect(id: String) = for {
    uuid <- ZIO.fromOption(try {
      Some(UUID.fromString(id))
    } catch {
      case e: IllegalArgumentException => None
    })
    _ <- UserRepository.deletePermission(uuid)
  } yield ()

  override def postItemRenderer(item: Permission): Html = item.htmlTableRowSwap

  override def listRenderer(permissions: List[Permission]): Html = {
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

object PermissionsPage
    extends SimplePage(Root / "permissions", SiteMap.tabs.setActiveTab(SiteMap.permissionsTab), PermissionEffects)
