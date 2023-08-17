package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.FormError.*
import fi.kimmoeklund.domain.Role
import fi.kimmoeklund.html._
import fi.kimmoeklund.service.UserRepository
import io.github.arainko.ducktape.*
import zio.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.html.*
import zio.http.{html as _, *}

import java.util.UUID
import scala.util.Try

case class RoleView(id: UUID, name: String, permissions: Seq[String]) extends Identifiable

object RoleView:
  def from(r: Role) = r
    .into[RoleView]
    .transform(Field.computed(_.permissions, r => r.permissions.map(p => s"${p.target} (${p.permission})")))
  given HtmlEncoder[RoleView] = HtmlEncoder.derived[RoleView]

case class RoleForm(
    name: String,
    foobar: String,
    @inputSelectOptions("permissions/options", "permissions", true) permissions: Seq[String]
)

object RoleForm:
  given HtmlEncoder[RoleForm] = HtmlEncoder.derived[RoleForm]

case class RolesPage(path: String, db: String)
    extends Page[UserRepository, Role, RoleView]
    with NewResourceForm[RoleForm] {
  val htmlId = path

  def listItems = for {
    repo <- ZIO.serviceAt[UserRepository](db)
    orgs <- repo.get.getRoles
  } yield orgs

  def mapToView = r => RoleView.from(r)

  def post(request: Request) = (for {
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
  } yield (mapToView(r))).map(newResourceHtml)

  override def delete(id: String) = for {
    repo <- ZIO.serviceAt[UserRepository](db)
    uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(InputValueInvalid("id", "unable to parse as UUID"))
    _ <- repo.get.deleteRole(uuid)
  } yield ()

  override def optionsList = listItems.map(roles => roles.map(r => option(r.name, valueAttr := r.id.toString)))
}
