package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.*
import fi.kimmoeklund.domain.FormError.*
import fi.kimmoeklund.html.*
import fi.kimmoeklund.html.encoder.*
import zio.*
import zio.http.*
import zio.http.template.*
import zio.prelude.Validation

import java.util.UUID
import scala.util.Try
import fi.kimmoeklund.repository.PermissionRepository
import fi.kimmoeklund.repository.QuillCtx
import fi.kimmoeklund.repository.PermissionRepositoryLive
import zio.http.template.Attributes.PartialAttribute
import play.twirl.api.HtmlFormat

case class PermissionForm(target: Option[String], @inputNumber permission: Option[Int])

case class ValidPermissionForm(target: String, permission: Int)

object PermissionFormValidators:
  def validatePermission(form: PermissionForm): Validation[FormError, ValidPermissionForm] =
    Validation.validateWith(
      Validation.fromOptionWith(Missing("target"))(form.target),
      Validation.fromOptionWith(Missing("permission"))(form.permission)
    )(ValidPermissionForm.apply)

case class PermissionsPage(path: Path, db: String, val name: String)
    extends CrudPage[PermissionRepository, Permission, Permission, PermissionForm]:
  val errorHandler        = DefaultErrorHandler("permission").handle;
  val formPropertyEncoder = summon[PropertyHtmlEncoder[PermissionForm]]
  val formValueEncoder    = summon[ValueHtmlEncoder[PermissionForm]]
  val viewPropertyEncoder = summon[PropertyHtmlEncoder[Permission]]
  val viewValueEncoder    = summon[ValueHtmlEncoder[Permission]]

  override def listItems(using QuillCtx) =
    for {
      repo  <- ZIO.service[PermissionRepository]
      items <- repo.getList
    } yield (items)
  override def mapToView = p => p

  override def upsertResource(using QuillCtx)(request: Request) =
    parseForm(request).flatMap(form =>
      (for {
        repo <- ZIO.service[PermissionRepository]
        validForm <- PermissionFormValidators.validatePermission(form).toZIO
        p    <- repo.add(validForm)
      } yield CreatedEntity(p)).mapErrorCause(e => Cause.fail(FormWithErrors(e.failures, Some(form))))
    )

  override def deleteInternal(using QuillCtx)(id: String) = (for {
    repo <- ZIO.service[PermissionRepository]
    uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
    _    <- repo.delete(PermissionId(uuid))
  } yield ()).mapError(errorHandler(_))

  override def renderAsOptions(using QuillCtx)(selected: Chunk[String] = Chunk.empty) =
    listItems
      .map(permissions => {
        val targetMap = permissions.groupMap(_.target)(p => p)
        Html.fromSeq(
          targetMap.keys.toList.map(t =>
            optgroup(
              labelAttr := t,
              targetMap(t).map(p =>
                option(
                  p.permission.toString,
                  valueAttr := p.id.toString,
                  if selected.contains(p.id.toString) then selectedAttr := "selected"
                  else emptyHtml
                )
              )
            )
          )
        )
      })
      .map(h => HtmlFormat.raw(h.encode.toString))
      .mapError(errorHandler(_))

  override def get(using QuillCtx)(id: String) = (for {
    repo          <- ZIO.service[PermissionRepository]
    uuid          <- ZIO.attempt(UUID.fromString(id)).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
    permissionOpt <- repo.getByIds(Set(PermissionId(uuid))).map(_.headOption)
    permission    <- ZIO.fromOption(permissionOpt).orElseFail(ExistingEntityError.EntityNotFound(id))
  } yield permission).mapError(errorHandler(_))
