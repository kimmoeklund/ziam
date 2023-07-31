package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.FormError.*
import fi.kimmoeklund.domain.Role
import fi.kimmoeklund.html.Page
import fi.kimmoeklund.service.UserRepository
import zio.*
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.{html as _, *}

import java.util.UUID
import scala.util.Try

case class RolesPage(path: String, db: String) extends Page[UserRepository] {
  extension (r: Role) {
    def htmlTableRow(db: String): Dom = tr(
      PartialAttribute("hx-target") := "this",
      PartialAttribute("hx-swap") := "delete",
      td(r.name),
      td(r.id.toString),
      td(r.permissions.map(p => s"${p.target} ${p.permission}").mkString(", ")),
      td(
        button(
          classAttr := "btn btn-danger" :: Nil,
          "Delete",
          PartialAttribute("hx-delete") := s"/$db/roles/${r.id}"
        )
      )
    )

    def htmlTableRowSwap(db: String): Dom =
      tBody(
        PartialAttribute("hx-swap-oob") := "beforeend:#roles-table",
        htmlTableRow(db)
      )
  }

  private def getRoles = for {
    repo <- ZIO.serviceAt[UserRepository](db)
    orgs <- repo.get.getRoles
  } yield orgs

  override def tableList = getRoles.map(roles => htmlTable(roles))

  def post(request: Request) = for {
    repo <- ZIO.serviceAt[UserRepository](db)
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
  } yield (r).htmlTableRowSwap(db)

  override def delete(id: String) = for {
    repo <- ZIO.serviceAt[UserRepository](db)
    uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(InputValueInvalid("id", "unable to parse as UUID"))
    _ <- repo.get.deleteRole(uuid)
  } yield ()

  def htmlTable(roles: List[Role]): Html = {
    table(
      classAttr := "table" :: Nil,
      tHead(
        tr(
          th("Role"),
          th("ID"),
          th("Permissions")
        )
      ),
      tBody(id := "roles-table", roles.map(_.htmlTableRow(db)))
    ) ++
      form(
        idAttr := "add-role",
        PartialAttribute("hx-post") := s"roles",
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
            PartialAttribute("hx-get") := s"/$db/permissions/options",
            PartialAttribute("hx-trigger") := "revealed",
            PartialAttribute("hx-target") := "#permissions-select",
            PartialAttribute("hx-swap") := "innerHTML"
          )
        ),
        button(typeAttr := "submit", classAttr := "btn" :: "btn-primary" :: Nil, "Add")
      ) ++
      script(srcAttr := "/scripts")
  }

  override def optionsList = getRoles.map(roles => roles.map(r => option(r.name, valueAttr := r.id.toString)))
}
