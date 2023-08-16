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

case class PermissionForm(target: String, @inputNumber permission: Int)

object PermissionForm:
  given HtmlEncoder[PermissionForm] = HtmlEncoder.derived[PermissionForm]

case class PermissionsPage(path: String, db: String) extends Page[UserRepository, Permission, Permission] with NewResourceForm[PermissionForm] {

  def listItems = for {
    repo <- ZIO.serviceAt[UserRepository](db)
    permissions <- repo.get.getPermissions
  } yield permissions

  def mapToView = p => p

  def post(request: Request) = (for {
    repo <- ZIO.serviceAt[UserRepository](db)
    form <- request.body.asURLEncodedForm.orElseFail(InputValueInvalid("body", "unable to parse as form"))
    target <- ZIO.fromTry(Try(form.get("target").get.stringValue.get)).orElseFail(MissingInput("target"))
    permission <- ZIO.fromTry(Try(form.get("permission").get.stringValue.get)).orElseFail(MissingInput("permission"))
    permissionInt <- ZIO
      .fromTry(Try(permission.toInt))
      .orElseFail(InputValueInvalid("permission", "unable to parse to integer"))
    p <- repo.get.addPermission(Permission(UUID.randomUUID(), target, permissionInt))
  } yield (p)).map(newResourceHtml)

  override def delete(id: String) = for {
    repo <- ZIO.serviceAt[UserRepository](db)
    uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(InputValueInvalid("id", "unable to parse as UUID"))
    _ <- repo.get.deletePermission(uuid)
  } yield ()

  override def optionsList = listItems.flatMap(permissions => {
    val targetMap = permissions.groupMap(_.target)(p => p)
    ZIO.succeed(
      targetMap.keys.toList.map(t =>
        optgroup(
          labelAttr := t,
          targetMap(t).map(p => option(p.permission.toString, valueAttr := p.id.toString))
        )
      )
    )
  })
}
