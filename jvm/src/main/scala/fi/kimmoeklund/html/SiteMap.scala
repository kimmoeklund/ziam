package fi.kimmoeklund.html

import fi.kimmoeklund.domain.Permission
import fi.kimmoeklund.html.pages.PermissionsPage
import zio.http.*
import fi.kimmoeklund.service.UserRepository
import zio.http.html.div

object SiteMap {
  val organizationsTab = Tab("Organizations", Root / "organizations")
  val usersTab = Tab("Users", Root / "users")
  val permissionsTab = Tab("Permissions", Root / "permissions")
  val rolesTab = Tab("Roles", Root / "roles")
  val tabs = TabMenu(
    List(
        organizationsTab,
        usersTab,
        permissionsTab,
        rolesTab
    )
  )
}
