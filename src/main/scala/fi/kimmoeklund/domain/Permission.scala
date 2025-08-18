package fi.kimmoeklund.domain

import zio.json.*

import java.util.UUID
import fi.kimmoeklund.domain.Identifiable
import fi.kimmoeklund.html.pages.PermissionForm
import scala.meta.common.Convert
import zio.prelude.Newtype
import scala.util.Try
import fi.kimmoeklund.html.inputNumber
import io.github.arainko.ducktape.Transformer

object PermissionId extends Newtype[UUID]:
  given Convert[PermissionId, String] with
    def apply(permissionId: PermissionId): String = permissionId.toString
  given Convert[Set[PermissionId], String] with
    def apply(permissionIds: Set[PermissionId]): String = permissionIds.mkString(",")
  given Convert[String, Option[PermissionId]] with
    def apply(permissionId: String): Option[PermissionId] =
      Try(java.util.UUID.fromString(permissionId)).toOption.map(PermissionId(_))
  given Transformer[UUID, PermissionId] = uuid => PermissionId(uuid)
  def create: PermissionId              = PermissionId(java.util.UUID.randomUUID())
  extension (permissionId: PermissionId)
    def compare(that: PermissionId) =
      PermissionId.unwrap(permissionId).toString().compare(PermissionId.unwrap(that).toString)

type PermissionId = PermissionId.Type

case class Permission(id: PermissionId, target: String, permission: Int)
    extends Identifiable
    with Ordered[Permission]
    with CrudResource[PermissionForm] {
  import scala.math.Ordered.orderingToOrdered
  def compare(that: Permission): Int = this.id.compare(that.id)
  val form                           = PermissionForm(Some(this.target), Some(this.permission))
}

object Permission:
  given JsonEncoder[Permission]   = DeriveJsonEncoder.gen[Permission]
  given JsonDecoder[Permission]   = DeriveJsonDecoder.gen[Permission]
  given JsonDecoder[PermissionId] = JsonDecoder[UUID].map(PermissionId(_))
  given JsonEncoder[PermissionId] = JsonEncoder[UUID].contramap(PermissionId.unwrap)
  given Convert[Set[Permission], String] with
    def apply(permissions: Set[Permission]) = permissions.map(p => s"${p.target} (${p.permission})").mkString("<br/>")
