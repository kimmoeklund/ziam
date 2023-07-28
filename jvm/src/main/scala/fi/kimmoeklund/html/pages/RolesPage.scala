package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.FormError.*
import fi.kimmoeklund.domain.Role
import fi.kimmoeklund.html.{Effects, Renderer}
import fi.kimmoeklund.service.UserRepository
import zio.*
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.{html as _, *}

import java.util.UUID
import scala.util.Try

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
    repo <- ZIO.serviceAt[UserRepository]("ziam")
    roles <- repo.get.getRoles
  } yield roles

  def postEffect(request: Request) = for {
    repo <- ZIO.serviceAt[UserRepository]("ziam")
    form <- request.body.asURLEncodedForm.orElseFail(InputValueInvalid("body", "unable to parse as form"))
    name <- ZIO.fromTry(Try(form.get("name").get.stringValue.get)).orElseFail(MissingInput("name"))
    inputUuids <- ZIO
      .fromTry(
        Try(
          form
            .get("permissions")
            .get
            .stringValue
            .get
            .split(",")
            .map(uuidStr => UUID.fromString(uuidStr))
        )
      )
      .mapError((e: Throwable) =>
        e match {
          case _: IllegalArgumentException => InputValueInvalid("permissions", "unable to parse as UUIDs")
          case _                           => MissingInput("permissions")
        }
      )
    permissions <- repo.get.getPermissionsById(inputUuids.toList)
    _ <- ZIO.logInfo(inputUuids.mkString(","))
    r <- repo.get.addRole(Role(UUID.randomUUID(), name, permissions))
  } yield r

  override def deleteEffect(id: String) = for {
    repo <- ZIO.serviceAt[UserRepository]("ziam")
    uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(InputValueInvalid("id", "unable to parse as UUID"))
    _ <- repo.get.deleteRole(uuid)
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
        div(
          classAttr := "mb-3" :: Nil,
          label(
            "Role name",
            forAttr := "name-field",
            classAttr := "form-label" :: Nil
          ),
          input(idAttr := "name-field", nameAttr := "name", classAttr := "form-control" :: Nil, typeAttr := "text")
        ),
        div(
          classAttr := "mb-3" :: Nil,
          label("Permissions", classAttr := "form-label" :: Nil, forAttr := "permissions-select"),
          select(
            idAttr := "permissions-select",
            classAttr := "form-select" :: Nil,
            multipleAttr := "multiple",
            nameAttr := "permissions",
            PartialAttribute("hx-params") := "none",
            PartialAttribute("hx-get") := "/permissions/options",
            PartialAttribute("hx-trigger") := "revealed",
            PartialAttribute("hx-target") := "#permissions-select",
            PartialAttribute("hx-swap") := "innerHTML"
          )
        ),
        button(typeAttr := "submit", classAttr := "btn" :: "btn-primary" :: Nil, "Add")
      ) ++
      script(srcAttr := "/scripts")
  }

  override def optionsList(args: List[Role]): Html =
    args.map(r => option(r.name, valueAttr := r.id.toString))
}
