package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.*
import fi.kimmoeklund.domain.FormError.ValueInvalid
import fi.kimmoeklund.html.*
import fi.kimmoeklund.html.forms.*
import fi.kimmoeklund.service.{ Repositories, UserRepository, RoleRepository }
import io.github.arainko.ducktape.*
import zio.*
import zio.http.html.*
import zio.http.{html as _, *}

import java.util.UUID

case class UserView(id: UUID, name: String, organization: String, roles: Seq[Role], logins: Seq[Login])
    extends Identifiable

object UserView:
  def from(u: User) = 
    UserView(u.id, u.name, u.organization.name, u.roles, u.logins)
  given HtmlEncoder[UserView] = HtmlEncoder.derived[UserView]

case class UsersPage(path: String, db: String)
    extends Page[UserRepository & RoleRepository, User, UserView]
    with NewResourceForm[UserForm]:

  def mapGetDataError(error: GetDataError, field: String): FormError = error match {
    case a: GetDataError.EntityNotFound[_] =>
      ValueInvalid(field, s"requested $field was not found, please reload the page and try again.")
    case _ =>
      FormError.ProcessingFailed(s"System failure while creating user. User was not created, please try again later.")
  }

  def mapInsertDataError(error: InsertDataError): FormError = error match {
    case InsertDataError.UniqueKeyViolation("userName") =>
      ValueInvalid("username", "Username is already taken, please select another one.")
    case _ =>
      FormError.ProcessingFailed(s"System failure while creating user. User was not created, please try again later.")
  }

  def mapToView = UserView.from(_)
  def post(req: Request) = {
    val userId = UUID.randomUUID()
    val parsedForm = for {
      form <- req.body.asURLEncodedForm
      userForm <- ZIO.succeed(
        UserForm.fromURLEncodedForm(
          form.get("name"),
          form.get("username"),
//          form.get("email"),
          form.get("password"),
          form.get("password_confirmation"),
          form.get("roles"),
          form.get("organization")
        )
      )

    } yield (userForm)
    parsedForm
      .orElseFail(FormError.ProcessingFailed("System error, unable to parse form"))
      .flatMap(form => {
        val effect = for {
          userRepo <- ZIO.serviceAt[UserRepository](db)
          roleRepo <- ZIO.serviceAt[RoleRepository](db)
          newUserForm <- ValidUserForm.from(form).toZIO
          orgAndRoles <- userRepo.get
            .getOrganizationById(newUserForm.organizationId)
            .mapError(mapGetDataError(_, "organization"))
            .validatePar(roleRepo.get.getRolesByIds(newUserForm.roleIds.toSeq).mapError(mapGetDataError(_, "roles")))
          user <- userRepo.get
            .addUser(
              NewPasswordUser(userId, newUserForm.name, orgAndRoles._1, newUserForm.credentials, orgAndRoles._2)
            ) // TODO new password user turha?
            .mapError(mapInsertDataError)
        } yield (user)
        effect
          .mapError({
            case FormError.Missing(field) => ErrorMsg(field, s"$field is mandatory")
            case FormError.PasswordsDoNotMatch =>
              ErrorMsg("password_confirmation", "password confirmation does not match")
            case ValueInvalid(field, details) => ErrorMsg(field, details)
            case e: FormError                 => ErrorMsg("", "System error processing the form")
          })
          .foldCause(error => 
              htmlForm(Some(form), if error.failures.nonEmpty then Some(error.failures) else None), user => newResourceHtml(mapToView(user)))
      })
  }

  override def delete(id: String): ZIO[Map[String, UserRepository], ErrorCode, Unit] = {
    for {
      uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
      repo <- ZIO.serviceAt[UserRepository](db)
      _ <- repo.get.deleteUser(uuid)
    } yield ()
  }

  override def optionsList(selected: Option[Seq[String]] = None) = ???

  def listItems = for {
    repo <- ZIO.serviceAt[UserRepository](db)
    users <- repo.get.getUsers
  } yield users
