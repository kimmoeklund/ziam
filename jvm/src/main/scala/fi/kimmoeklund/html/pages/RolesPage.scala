package fi.kimmoeklund.html.pages

import zio.*
import zio.http.{html as _, *}
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import fi.kimmoeklund.html.{Effects, Renderer, SimplePage, SiteMap}
import fi.kimmoeklund.service.UserRepository

object RolesEffects extends Effects[UserRepository, Unit] with Renderer[Unit]:
  override def listRenderer(args: List[Unit]): Html = ???

  override def postItemRenderer(item: Unit): Html = ???

  override def getEffect: ZIO[UserRepository, Throwable, List[Unit]] = ???

  override def postEffect(req: Request): ZIO[UserRepository, Option[Nothing] | Throwable, Unit] = ???

  override def deleteEffect(id: String): ZIO[UserRepository, Option[Nothing] | Throwable, Unit] = ???

object RolesPage extends SimplePage(Root / "roles", SiteMap.tabs.setActiveTab(SiteMap.rolesTab), RolesEffects)
