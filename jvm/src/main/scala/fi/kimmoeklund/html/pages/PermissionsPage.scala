package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.FormError.*
import fi.kimmoeklund.domain.Permission
import fi.kimmoeklund.html.*
import fi.kimmoeklund.service.UserRepository
import zio.*
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.{html as _, *}

import java.util.UUID
import scala.util.Try

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
    repo <- ZIO.serviceAt[UserRepository]("ziam")
    permissions <- repo.get.getPermissions
  } yield permissions

  def postEffect(request: Request) = for {
    repo <- ZIO.serviceAt[UserRepository]("ziam")
    form <- request.body.asURLEncodedForm.orElseFail(InputValueInvalid("body", "unable to parse as form"))
    target <- ZIO.fromTry(Try(form.get("target").get.stringValue.get)).orElseFail(MissingInput("target"))
    permission <- ZIO.fromTry(Try(form.get("permission").get.stringValue.get)).orElseFail(MissingInput("permission"))
    permissionInt <- ZIO
      .fromTry(Try(permission.toInt))
      .orElseFail(InputValueInvalid("permission", "unable to parse to integer"))
    p <- repo.get.addPermission(Permission(UUID.randomUUID(), target, permissionInt))
  } yield p

  override def deleteEffect(id: String) = for {
    repo <- ZIO.serviceAt[UserRepository]("ziam")
    uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(InputValueInvalid("id", "unable to parse as UUID"))
    _ <- repo.get.deletePermission(uuid)
  } yield ()

  override def postResult(item: Permission): Html = item.htmlTableRowSwap

  override def optionsList(args: List[Permission]): Html =
    val targetMap = args.groupMap(_.target)(p => p)
    targetMap.keys.toList.map(t =>
      optgroup(
        labelAttr := t,
        targetMap(t).map(p => option(p.permission.toString, valueAttr := p.id.toString))
      )
    )

  override def htmlTable(permissions: List[Permission]): Html = {
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
