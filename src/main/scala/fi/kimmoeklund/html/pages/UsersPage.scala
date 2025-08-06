package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.*
import fi.kimmoeklund.domain.FormError.ValueInvalid
import fi.kimmoeklund.html.*
import fi.kimmoeklund.html.encoder.*
import fi.kimmoeklund.html.forms.*
import fi.kimmoeklund.repository.*
import io.github.arainko.ducktape.*
import zio.http.Request
import zio.http.template.*
import zio.{Cause, Chunk, ZIO}

import java.util.UUID
import scala.annotation.threadUnsafe

import fi.kimmoeklund.html.encoder.FormDecoder.given
import play.twirl.api.HtmlFormat
import zio.http.Path

case class UserView(id: UserId, name: String, roles: Set[Role], logins: Set[Login]) extends Identifiable

object UserView:
  def from(u: User) =
    UserView(u.id, u.name, u.roles, u.logins)

case class UsersPage(path: Path, db: String, name: String)
    extends CrudPage[UserRepositoryLive & RoleRepository, User, UserView, UserForm]:
  override val formValueEncoder                                   = summon[ValueHtmlEncoder[UserForm]]
  override val formPropertyEncoder                                = summon[PropertyHtmlEncoder[UserForm]]
  override val viewValueEncoder: ValueHtmlEncoder[UserView]       = summon[ValueHtmlEncoder[UserView]]
  override val viewPropertyEncoder: PropertyHtmlEncoder[UserView] = summon[PropertyHtmlEncoder[UserView]]
  override val errorHandler                                       = DefaultErrorHandler(name).handle;

  def mapToView = r => UserView.from(r)

  def createOrUpdate(using
      QuillCtx
  )(form: ValidUserForm | ValidNewUserForm, userRepo: UserRepositoryLive, roles: Set[Role]) = form match
    case ValidNewUserForm(id, name, roleIds, credentials) =>
      userRepo
        .add(NewPasswordUser(id, name, credentials, roles))
    case ValidUpdateUserForm(id, name, roleIds, newPassword) =>
      userRepo
        .update(User(id, name, roles, Set.empty))

  def upsertResource(using QuillCtx)(req: Request) = {
    val parsedForm = parseForm(req)
    parsedForm
      .flatMap(form => {
        (for {
          userRepo <- ZIO.service[UserRepositoryLive]
          roleRepo <- ZIO.service[RoleRepository]
          validForm <-
            if form.id.isEmpty then FormValidators.newUser(form).toZIO else FormValidators.updateUser(form).toZIO
          roles <- roleRepo.getByIds(validForm.roles)
          user  <- createOrUpdate(validForm, userRepo, roles)
        } yield user).mapErrorCause(e => Cause.fail(FormWithErrors(e.failures, Some(form))))
      })
  }

  def deleteInternal(using QuillCtx)(id: String) =
    (for {
      userId <- ZIO.attempt(UserId(UUID.fromString(id))).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
      repo   <- ZIO.service[UserRepositoryLive]
      _      <- repo.delete(userId)
    } yield ()).mapError(errorHandler(_))

  def get(using QuillCtx)(id: String) = (for {
    userId  <- ZIO.attempt(UserId(UUID.fromString(id))).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
    repo    <- ZIO.service[UserRepositoryLive]
    userOpt <- repo.getList(Some(userId)).map(_.headOption)
    user    <- ZIO.fromOption(userOpt).orElseFail(ExistingEntityError.EntityNotFound(id))
  } yield (user)).mapError(errorHandler(_))

  override def renderAsOptions(using QuillCtx)(selected: Chunk[String] = Chunk.empty) =
    listItems
      .map(users =>
        Html.fromSeq(
          users.map(user =>
            option(
              user.name,
              valueAttr := user.id.toString,
              if selected.contains(UserId.unwrap(user.id).toString) then selectedAttr := "true"
              else emptyHtml
            )
          )
        )
      )
      .map(h => HtmlFormat.raw(h.encode.toString))
      .mapError(errorHandler(_))

  def listItems(using QuillCtx) = for {
    repo  <- ZIO.service[UserRepositoryLive]
    users <- repo.getList(None)

  } yield users
