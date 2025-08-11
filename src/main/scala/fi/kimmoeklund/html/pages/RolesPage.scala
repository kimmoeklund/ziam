package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.FormError.{ValueInvalid, Missing}
import fi.kimmoeklund.domain.*
import fi.kimmoeklund.html.*
import fi.kimmoeklund.html.encoder.*
import fi.kimmoeklund.repository.*
import fi.kimmoeklund.service.DbManagement
import io.github.arainko.ducktape.*
import zio.http.Request
import zio.{Chunk, ZIO, Cause}
import zio.prelude.Validation

import java.util.UUID
import scala.util.Try
import scala.meta.common.Convert
import play.twirl.api.HtmlFormat
import zio.http.template.*
import zio.http.Path

case class RoleView(id: RoleId, name: String, permissions: Set[Permission]) extends Identifiable

object RoleView:
  def from(r: Role) = r
    .to[RoleView]

case class RoleForm(
    name: Option[String],
    @inputSelectOptions("/page/permissions/options", "permissions", true)
    permissions: Option[Set[PermissionId]]
)

case class ValidRoleForm(name: String, permissions: Set[PermissionId])

object RoleFormValidators:
  def validateRole(form: RoleForm): Validation[FormError, ValidRoleForm] =
    Validation.validateWith(
      Validation.fromOptionWith(Missing("name"))(form.name),
      Validation.succeed(form.permissions.getOrElse(Set.empty))
    )(ValidRoleForm.apply)

case class RolesPage(path: Path, db: String, val name: String)
    extends CrudPage[RoleRepository & PermissionRepository, Role, RoleView, RoleForm]:
  import PropertyHtmlEncoder.given
  import ValueHtmlEncoder.given
  override protected val formValueEncoder: ValueHtmlEncoder[RoleForm]       = summon[ValueHtmlEncoder[RoleForm]]
  override protected val formPropertyEncoder: PropertyHtmlEncoder[RoleForm] = summon[PropertyHtmlEncoder[RoleForm]]
  override protected val viewValueEncoder: ValueHtmlEncoder[RoleView]       = summon[ValueHtmlEncoder[RoleView]]
  override protected val viewPropertyEncoder: PropertyHtmlEncoder[RoleView] = summon[PropertyHtmlEncoder[RoleView]]
  val errorHandler                                                          = DefaultErrorHandler("role").handle

  def listItems(using QuillCtx) =
    for {
      repo  <- ZIO.service[RoleRepository]
      items <- repo.getList
    } yield (items)

  def mapToView = r => RoleView.from(r)

  def upsertResource(using QuillCtx)(request: Request) = parseForm(request).flatMap(form =>
    (for {
      roleRepo <- ZIO.service[RoleRepository]
      _        <- ZIO.log(s"Form: $form")
      validation = RoleFormValidators.validateRole(form)
      _         <- ZIO.log(s"Validation result: $validation")
      validForm <- validation.toZIO.tapError(e => ZIO.log(s"Validation failed with: $e"))
      _         <- ZIO.log(s"Valid form created: $validForm")
      r         <- roleRepo.add(validForm)
    } yield CreatedEntity(r))
      .tapErrorCause(e => ZIO.log(s"form: ${form.toString} has failures: ${e.failures}"))
      .mapErrorCause(e => {
       Cause.fail(FormWithErrors(e.failures, Some(form)))
    })
  )

  def deleteInternal(using QuillCtx)(id: String) = (for {
    repo <- ZIO.service[RoleRepository]
    uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
    _    <- repo.delete(RoleId(uuid))
  } yield ()).mapError(e => errorHandler(e))

  override def renderAsOptions(using QuillCtx)(selected: Chunk[String] = Chunk.empty) =
    listItems
      .map(roles =>
        Html.fromSeq(
          roles.map(r =>
            option(
              r.name,
              valueAttr := r.id.toString,
              if selected.contains(RoleId.unwrap(r.id).toString) then selectedAttr := "true"
              else emptyHtml
            )
          )
        )
      )
      .map(h => HtmlFormat.raw(h.encode.toString))
      .mapError(errorHandler(_))

  def get(using QuillCtx)(id: String) = (for {
    roleId  <- ZIO.attempt(RoleId(UUID.fromString(id))).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
    repo    <- ZIO.service[RoleRepository]
    roleOpt <- repo.getByIds(Set(roleId)).map(_.headOption)
    role    <- ZIO.fromOption(roleOpt).orElseFail(ExistingEntityError.EntityNotFound(id))
  } yield (role)).mapError(errorHandler(_))
