package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.FormError.*
import fi.kimmoeklund.domain.*
import fi.kimmoeklund.html.*
import fi.kimmoeklund.service.{PermissionRepository}
import zio.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.html.*
import zio.http.{html as _, *}

import java.util.UUID
import scala.util.Try

case class PermissionForm(target: String, @inputNumber permission: Int)

object PermissionForm:
  given HtmlEncoder[PermissionForm] = HtmlEncoder.derived[PermissionForm]

case class PermissionsPage(path: String, db: String)
    extends CrudPage[PermissionRepository, Permission, Permission, PermissionForm]:

  def listItems = for {
    repo        <- ZIO.serviceAt[PermissionRepository](db)
    permissions <- repo.get.getPermissions
  } yield permissions

  def mapToView = p => p.resource
  def emptyForm = PermissionForm("", 0)

  override def upsertResource(request: Request) = (for {
    repo   <- ZIO.serviceAt[PermissionRepository](db)
    form   <- request.body.asURLEncodedForm.orElseFail(ValueInvalid("body", "unable to parse as form"))
    target <- form.zioFromField("target")
    permissionInt <- form
      .zioFromField("permission")
      .map(_.toInt)
      .orElseFail(ValueInvalid("permission", "unable to parse to integer"))
    p <- repo.get.addPermission(Permission(UUID.randomUUID(), target, permissionInt))
  } yield (p)).map(newResourceHtml)

  override def delete(id: String) = for {
    repo <- ZIO.serviceAt[PermissionRepository](db)
    uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
    _    <- repo.get.deletePermission(uuid)
  } yield ()

  override def optionsList(selected: Option[Seq[String]] = None) = listItems.flatMap(permissions => {
    val targetMap = permissions.groupMap(_.resource.target)(p => p)
    ZIO.succeed(
      targetMap.keys.toList.map(t =>
        optgroup(
          labelAttr := t,
          targetMap(t).map(p =>
            option(
              p.resource.permission.toString,
              valueAttr := p.resource.id.toString,
              if selected.isDefined && selected.get.contains(p.resource.id.toString) then selectedAttr := "selected"
              else emptyHtml
            )
          )
        )
      )
    )
  })

  override def get(id: String) = for {
    repo          <- ZIO.serviceAt[PermissionRepository](db)
    uuid          <- ZIO.attempt(UUID.fromString(id)).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
    permissionOpt <- repo.get.getPermissionsByIds(Seq(uuid)).map(_.headOption)
    permission    <- ZIO.fromOption(permissionOpt).orElseFail(ExistingEntityError.EntityNotFound(id))
  } yield permission
