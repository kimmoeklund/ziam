package fi.kimmoeklund.html.pages

import zio.*
import zio.http.{html as _, *}
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import fi.kimmoeklund.html.{SimplePage, SiteMap}
import fi.kimmoeklund.service.UserRepository
import fi.kimmoeklund.html.Effects

object UserEffects extends Effects[Nothing, Unit]:

  override def listRenderer(args: List[Unit]): Html = ???

  override def postItemRenderer(item: Unit): Html = ???

  override def postEffect(req: Request): ZIO[Nothing, Option[Nothing] | Throwable, Unit] = ???

  override def deleteEffect(id: String): ZIO[Nothing, Option[Nothing] | Throwable, Unit] = ???

  def getEffect = ZIO.succeed(List(()))

object UsersPage extends SimplePage(Root / "users", SiteMap.tabs.setActiveTab(SiteMap.usersTab), UserEffects)
