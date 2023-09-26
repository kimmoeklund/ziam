package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.FormError.*
import fi.kimmoeklund.domain.Role
import fi.kimmoeklund.domain.RoleId
import fi.kimmoeklund.html._
import fi.kimmoeklund.service.{Repositories, RoleRepository, PermissionRepository}
import io.github.arainko.ducktape.*
import zio.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.html.*
import zio.http.{html as _, *}

import java.util.UUID
import scala.util.Try
import fi.kimmoeklund.domain.Permission
import fi.kimmoeklund.service.DbManagement

case class RoleView(id: RoleId, name: String, permissions: Seq[Permission]) extends Identifiable

object RoleView:
  def from(r: Role) = r
    .to[RoleView]
  given HtmlEncoder[RoleView] = HtmlEncoder.derived[RoleView]

type PermissionId = String

case class RoleForm(
    name: String,
    @inputSelectOptions("permissions/options", "permissions", true) permissions: Seq[PermissionId]
)

object RoleForm:
  given HtmlEncoder[RoleForm] = HtmlEncoder.derived[RoleForm]
  given HtmlEncoder[Seq[PermissionId]] with {
    override def encodeValues(
        template: ElementTemplate,
        value: Seq[PermissionId],
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ) =
      HtmlEncoder[String].encodeValues(template, value.mkString(","))
    override def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any]) =
      HtmlEncoder[String].encodeParams(template, "permissions")
  }

case class RolesPage(path: String, db: String) extends Page[RoleRepository & PermissionRepository, Role, RoleView] with NewResourceForm[RoleForm] {
  val htmlId = path

  def listItems = for {
    repo <- ZIO.serviceAt[RoleRepository](db)
    orgs <- repo.get.getRoles
  } yield orgs

  def mapToView = r => RoleView.from(r)

  def post(request: Request) = (for {
    roleRepo <- ZIO.serviceAt[RoleRepository](db)
    permissionRepo <- ZIO.serviceAt[PermissionRepository](db)
    form <- request.body.asURLEncodedForm.orElseFail(ValueInvalid("body", "unable to parse as form"))
    name <- ZIO.fromTry(Try(form.get("name").get.stringValue.get)).orElseFail(Missing("name"))
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
          case _: IllegalArgumentException => ValueInvalid("permissions", "unable to parse as UUIDs")
          case _                           => Missing("permissions")
        }
      )
    permissions <- permissionRepo.get.getPermissionsByIds(inputUuids.toList)
    r <- roleRepo.get.addRole(Role(RoleId(UUID.randomUUID()), name, permissions))
  } yield (mapToView(r))).map(newResourceHtml)

  override def delete(id: String) = for {
    repo <- ZIO.serviceAt[RoleRepository](db)
    uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
    _ <- repo.get.deleteRole(uuid)
  } yield ()

  override def optionsList(selected: Option[Seq[String]] = None) = listItems.map(roles =>
    roles.map(r =>
      option(
        r.name,
        valueAttr := r.id.toString,
        if selected.isDefined && selected.get.contains(RoleId.unwrap(r.id).toString) then 
          selectedAttr := "true"
        else emptyHtml
      )
    )
  )
}
