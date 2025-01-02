package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.*
import fi.kimmoeklund.domain.FormError.ValueInvalid
import fi.kimmoeklund.html.*
import fi.kimmoeklund.html.forms.*
import fi.kimmoeklund.service.{Repositories, UserRepository, RoleRepository}
import io.github.arainko.ducktape.*
import zio.*
import zio.http.html.*
import zio.http.{html as _, *}

import java.util.UUID
import scala.annotation.threadUnsafe

case class UserView(id: UserId, name: String, roles: Set[Role], logins: Seq[Login]) extends Identifiable

object UserView:
  def from(u: User) =
    UserView(u.id, u.name, u.roles, u.logins)
  given HtmlEncoder[UserView] = HtmlEncoder.derived[UserView]

case class UsersPage(path: String, db: String)
    extends CrudPage[UserRepository & RoleRepository, User, UserView, UserForm]:

  override def emptyForm = UserForm(None, None, None, None, None, None)

  def mapExistingEntityError(error: ExistingEntityError, field: String): FormError = error match {
    case a: ExistingEntityError.EntityNotFound[_] =>
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

  def mapFormError(error: ErrorCode) = error match {
    case FormError.Missing(field) => ErrorMsg(field, s"$field is mandatory")
    case FormError.PasswordsDoNotMatch =>
      ErrorMsg("password_confirmation", "password confirmation does not match")
    case ValueInvalid(field, details) => ErrorMsg(field, details)
    case _                            => ErrorMsg("", "System error processing the form")
  }

  def parseForm(request: Request) = for {
    form <- request.body.asURLEncodedForm
    userForm <- ZIO.succeed(
      UserForm.fromURLEncodedForm(
        form.get("id"),
        form.get("name"),
        form.get("username"),
        form.get("password"),
        form.get("password_confirmation"),
        form.get("roles")
      )
    )
  } yield (userForm)

  def mapToView = r => UserView.from(r.resource)

  // move to a trait as can be generalized
  def foldErrors = (form: UserForm) =>
    (error: Cause[ErrorMsg]) =>
      Html.fromDomElement(
        div(
          idAttr := "form-response",
          htmlForm(Some(form), if error.failures.nonEmpty then Some(error.failures) else None)
        )
      )
  def foldSuccess = (user: User) => newResourceHtml(user)

  def createOrUpdate(form: ValidUserForm | ValidNewUserForm, userRepo: UserRepository, roles: Set[Role]) = form match
    case ValidNewUserForm(id, name, roleIds, credentials) =>
      userRepo
        .addUser(NewPasswordUser(UserId(id), name, credentials, roles))
        .mapError(mapInsertDataError)
    case ValidUpdateUserForm(id, name, roleIds, newPassword) =>
      userRepo
        .updateUser(User(id, name, roles, Seq.empty))
        .mapError(mapExistingEntityError(_, "user"))

  def upsertResource(req: Request) = {
    val parsedForm = parseForm(req)
    parsedForm
      .orElseFail(FormError.ProcessingFailed("System error, unable to parse form"))
      .flatMap(form => {
        (for {
          userRepo <- ZIO.serviceAt[UserRepository](db)
          roleRepo <- ZIO.serviceAt[RoleRepository](db)
          validForm <-
            if form.id.isEmpty then FormValidators.newUser(form).toZIO else FormValidators.updateUser(form).toZIO
          roles <- roleRepo.get.getRolesByIds(validForm.roles)
          user <- createOrUpdate(validForm, userRepo.get, roles)
        } yield (user))
          .tapErrorCause(e => ZIO.logError(s"Error: ${e.failures}, form: ${form}"))
          .mapError(mapFormError)
          .foldCause(foldErrors(form), foldSuccess)
      })
  }

  override def delete(id: String): ZIO[Map[String, UserRepository], ErrorCode, Unit] = {
    for {
      userId <- ZIO.attempt(UserId(UUID.fromString(id))).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
      repo   <- ZIO.serviceAt[UserRepository](db)
      _      <- repo.get.deleteUser(userId)
    } yield ()
  }

  override def get(id: String) = for {
    userId  <- ZIO.attempt(UserId(UUID.fromString(id))).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
    repo    <- ZIO.serviceAt[UserRepository](db)
    userOpt <- repo.get.getUsers(Some(userId)).map(_.headOption)
    user    <- ZIO.fromOption(userOpt).orElseFail(ExistingEntityError.EntityNotFound(id))
  } yield (user)

  override def optionsList(selected: Option[Seq[String]] = None) = ???

  def listItems = for {
    repo  <- ZIO.serviceAt[UserRepository](db)
    users <- repo.get.getUsers(None)
  } yield users
