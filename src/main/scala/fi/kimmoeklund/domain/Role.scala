package fi.kimmoeklund.domain

import fi.kimmoeklund.domain.Identifiable
import zio.prelude.Newtype

import java.util.UUID
import scala.meta.common.Convert
import zio.json.JsonEncoder
import zio.json.DeriveJsonEncoder
import zio.json.DeriveJsonDecoder
import zio.json.JsonDecoder
import scala.util.Try
import scala.compiletime.{erasedValue}
import fi.kimmoeklund.html.inputSelectOptions
import fi.kimmoeklund.html.pages.RoleForm

object RoleId extends Newtype[UUID]:
  given Convert[RoleId, String] with
    def apply(roleId: RoleId): String = roleId.toString
  given Convert[Set[RoleId], String] with
    def apply(roleIds: Set[RoleId]): String = roleIds.mkString(",")
  given Convert[String, Option[RoleId]] with
    def apply(roleId: String): Option[RoleId] =
      Try(java.util.UUID.fromString(roleId)).toOption.map(RoleId(_))
  def create: RoleId = RoleId(java.util.UUID.randomUUID())

type RoleId = RoleId.Type

case class Role(id: RoleId, name: String, permissions: Set[Permission]) extends CrudResource[RoleForm]:
  override val form = RoleForm(Some(this.name), Some(this.permissions.map(_.id)))

object Role:
  given JsonEncoder[Role]   = DeriveJsonEncoder.gen[Role]
  given JsonDecoder[Role]   = DeriveJsonDecoder.gen[Role]
  given JsonDecoder[RoleId] = JsonDecoder[UUID].map(RoleId(_))
  given JsonEncoder[RoleId] = JsonEncoder[UUID].contramap(RoleId.unwrap)
  given Convert[Set[Role], String] with
    def apply(roles: Set[Role]): String = roles.map(v => s"${v.name}").mkString(",")
