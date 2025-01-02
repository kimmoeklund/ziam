package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.*
import fi.kimmoeklund.domain.FormError.ValueInvalid
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
import fi.kimmoeklund.domain.CrudResource

case class RoleView(id: RoleId, name: String, permissions: Seq[Permission]) extends Identifiable

object RoleView:
  def from(r: Role) = r
    .to[RoleView]
  given HtmlEncoder[RoleView] = HtmlEncoder.derived[RoleView]

type PermissionId = String

case class RoleForm(
    name: String,
    @inputSelectOptions("permissions/options", "permissions", true)
    permissions: Seq[PermissionId]
)

object RoleForm:
  given HtmlEncoder[RoleForm] = HtmlEncoder.derived[RoleForm]
  given HtmlEncoder[Seq[PermissionId]] with {
    override def encodeValues(
        template: ElementTemplate,
        value: Seq[PermissionId],
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any],
    ) =
      HtmlEncoder[String].encodeValues(template, value.mkString(","), errors, paramName, annotations)
    override def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any],  
        value: Option[Seq[PermissionId]] = None) =
      HtmlEncoder[String].encodeParams(template, "permissions", annotations)
  }

case class RolesPage(path: String, db: String) extends CrudPage[RoleRepository & PermissionRepository, Role, RoleView, RoleForm]:
  val htmlId = path

  def emptyForm = RoleForm("", Seq.empty)

  def listItems = for {
    repo <- ZIO.serviceAt[RoleRepository](db)
    orgs <- repo.get.getRoles
  } yield orgs

  def mapToView = r => RoleView.from(r.resource)

  override def upsertResource(request: Request) = (for {
    roleRepo <- ZIO.serviceAt[RoleRepository](db)
    permissionRepo <- ZIO.serviceAt[PermissionRepository](db)
    form <- request.body.asURLEncodedForm.orElseFail(ValueInvalid("body", "unable to parse as form"))
    name <- form.zioFromField("name")
    inputUuids <- form.zioFromField("permissions")
            .map(_.split(","))
            .map(_.map(UUID.fromString(_))).orElseFail(ValueInvalid("permissions", "unable to parse as UUIDs"))
    permissions <- permissionRepo.get.getPermissionsByIds(inputUuids.toList)
    r <- roleRepo.get.addRole(Role(RoleId(UUID.randomUUID()), name, permissions))
  } yield (newResourceHtml(r))) 

  override def delete(id: String) = for {
    repo <- ZIO.serviceAt[RoleRepository](db)
    uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
    _ <- repo.get.deleteRole(uuid)
  } yield ()

  override def optionsList(selected: Option[Seq[String]] = None) = listItems.map(roles =>
    roles.map(r =>
      option(
        r.resource.name,
        valueAttr := r.resource.id.toString,
        if selected.isDefined && selected.get.contains(RoleId.unwrap(r.resource.id).toString) then 
          selectedAttr := "true"
        else emptyHtml
      )
    )
  )

  override def get(id: String) = for {
    roleId <- ZIO.attempt(RoleId(UUID.fromString(id))).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
    repo <- ZIO.serviceAt[RoleRepository](db)
    roleOpt <- repo.get.getRolesByIds(Set(roleId)).map(_.headOption)
    role <- ZIO.fromOption(roleOpt).orElseFail(ExistingEntityError.EntityNotFound(id))
  } yield (role)
