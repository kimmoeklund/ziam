package fi.kimmoeklund.html.permission

import fi.kimmoeklund.domain.Permission
import fi.kimmoeklund.html.{htmlSnippet, htmxHead}
import fi.kimmoeklund.html.permission.PermissionsPage.permissionTable
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.{html as _, *}
import fi.kimmoeklund.service.UserRepository
import zio.*

import java.util.UUID

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
      tr(
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
    )
}

object PermissionsPage {
  def permissionTable(permissions: List[Permission]): Html = {
    h3("Permissions") ++
      table(
        classAttr := "table" :: Nil,
        tHead(
          tr(
            th("Target"),
            th("Permission")
          )
        ),
        tBody(id := "permissions-table", permissions.map(htmlTableRow))
      ) ++ h3("Add permission") ++
      form(
        idAttr := "add-permission",
        PartialAttribute("hx-post") := "/permissions",
        PartialAttribute("hx-swap") := "none",
        label("Target", forAttr := "target",
          input(idAttr := "target", nameAttr := "target", classAttr := "form-control" :: Nil, typeAttr := "text")
        ),
        label("Permission", forAttr := "permission",
          input(
            id := "permission",
            nameAttr := "permission",
            classAttr := "form-control" :: Nil,
            typeAttr := "text"
        )),
        button(typeAttr := "submit", classAttr := "btn" :: "btn-primary" :: Nil, "Add")
      ) ++
      script(srcAttr := "/scripts")
  }

  def buildHtml(permissions: List[Permission]): Dom = {
    html(
      htmxHead ++ body(
        div(classAttr := "container" :: Nil, h2("Permissions"), permissionTable(permissions))
      )
    )
  }

  def apply(): HttpApp[UserRepository, Nothing] = Http.collectZIO[Request] {
    case req@Method.POST -> Root / "permissions" =>
      val effect = for {
        form <- req.body.asURLEncodedForm
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
      effect.foldZIO(_ => ZIO.succeed(Response.status(Status.BadRequest)), p => ZIO.succeed(htmlSnippet(p.htmlTableRowSwap).addHeader("HX-Trigger-After-Swap", "myEvent")))
    case Method.GET -> Root / "permissions" =>
      val effect = for {
        permissions <- UserRepository.getPermissions()
      } yield permissions
      effect.foldZIO(
        _ => ZIO.succeed(Response.status(Status.InternalServerError)),
        (permissions: List[Permission]) => ZIO.succeed(Response.html(PermissionsPage.buildHtml(permissions)))
      )

    case Method.DELETE -> Root / "permissions" / id =>
      val effect = for {
        uuid <- ZIO.fromOption(try {
          Some(UUID.fromString(id))
        } catch {
          case e: IllegalArgumentException => None
        })
        _ <- UserRepository.deletePermission(uuid)
      } yield ()
      effect.foldZIO(_ => ZIO.succeed(Response.status(Status.BadRequest)), _ => ZIO.succeed(Response.status(Status.Ok)))
  }
}
