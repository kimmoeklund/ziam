package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.FormError.InputValueInvalid
import fi.kimmoeklund.domain.*
import fi.kimmoeklund.html.*
import fi.kimmoeklund.html.forms.*
import fi.kimmoeklund.service.UserRepository
import io.github.arainko.ducktape.*
import zio.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.html.*
import zio.http.{html as _, *}

import java.util.UUID

case class UserView(id: UUID, name: String, organization: String, roles: Seq[String], logins: Seq[Login])
    extends Identifiable

case class UserForm(
    name: String,
    username: String,
    @inputEmail email: String,
    @inputPassword password: String,
    @inputPassword password_confirmation: String,
    @inputSelectOptions("roles/options", "roles", true)
    roles: Seq[String],
    @inputSelectOptions("organizations/options", "organization", true)
    organization: String
)

object UserForm:
  given HtmlEncoder[UserForm] = HtmlEncoder.derived[UserForm]

object UserView:
  def from(u: User) = u
    .into[UserView]
    .transform(
      Field.computed(_.organization, u => u.organization.name),
      Field.computed(_.roles, u => u.roles.map(_.name))
    )
  given HtmlEncoder[LoginType] = HtmlEncoder.derived[LoginType]
  given HtmlEncoder[Login] = HtmlEncoder.derived[Login]
  given HtmlEncoder[Role] = HtmlEncoder.derived[Role]
  given HtmlEncoder[UserView] = HtmlEncoder.derived[UserView]

case class UsersPage(path: String, db: String)
    extends Page[UserRepository, User, UserView]
    with NewResourceForm[UserForm]:
  def mapToView = UserView.from(_)

  def post(req: Request) = {
    val userId = UUID.randomUUID()
    (for {
      repo <- ZIO.serviceAt[UserRepository](db)
      form <- req.body.asURLEncodedForm.orElseFail(InputValueInvalid("body", "unable to parse as form"))
      newUserForm <- NewUserForm
        .fromOptions(
          form.get("name"),
          form.get("organization"),
          form.get("roles"),
          form.get("username"),
          form.get("password"),
          form.get("password_confirmation")
        )
        .toZIO
      orgAndRoles <- repo.get
        .getOrganizationById(newUserForm.organizationId)
        .validatePar(repo.get.getRolesByIds(newUserForm.roleIds.toSeq))
      user <- repo.get.addUser(
        NewPasswordUser(userId, newUserForm.name, orgAndRoles._1, newUserForm.credentials, orgAndRoles._2)
      )
    } yield (mapToView(user))).map(newResourceHtml)
  }

  override def delete(id: String): ZIO[Map[String, UserRepository], ErrorCode, Unit] = {
    for {
      uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(InputValueInvalid("id", "unable to parse as UUID"))
      repo <- ZIO.serviceAt[UserRepository](db)
      _ <- repo.get.deleteUser(uuid)
    } yield ()
  }

  def optionsList = ???

  def tableList = for {
    repo <- ZIO.serviceAt[UserRepository](db)
    users <- repo.get.getUsers
  } yield htmlTable(users)

  def listItems = for {
    repo <- ZIO.serviceAt[UserRepository](db)
    users <- repo.get.getUsers
  } yield users
