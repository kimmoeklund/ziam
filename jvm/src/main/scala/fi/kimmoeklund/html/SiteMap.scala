package fi.kimmoeklund.html

import fi.kimmoeklund.domain.Permission
import fi.kimmoeklund.html.pages.PermissionsPage
import zio.http.*
import fi.kimmoeklund.service.UserRepository
import zio.http.html.div

object SiteMap {
  val usersTab = Tab("Users", Root / "users")
  val permissionsTab = Tab("Permissions", Root / "permissions")
  val rolesTab = Tab("Roles", Root / "roles")
  val tabs = TabMenu(
    List(
        usersTab,
        permissionsTab,
        rolesTab
    )
  )
//  val permissionsPage: SimplePage[UserRepository, List[Permission]] = SimplePage(Root / "permissions", tabs.setActiveTab(permissionsTab), PermissionsPage)
//  val usersPage: SimplePage[UserRepository, Unit] = SimplePage(Root / "users", tabs.setActiveTab(usersTab))
//  val rolesPage: SimplePage[UserRepository, Unit] = SimplePage(Root / "roles", tabs.setActiveTab(rolesTab))

  //def pages = List(permissionsPage, usersPage, rolesPage)

}
