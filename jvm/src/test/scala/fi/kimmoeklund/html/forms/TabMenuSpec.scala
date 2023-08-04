package fi.kimmoeklund.html.forms

import zio.test.* 
import fi.kimmoeklund.html.TabMenu
import fi.kimmoeklund.html.Tab
import zio.http.*
import io.getquill.util.testLoad

val tabMenu = TabMenu(List(Tab("test", Root / "test", false), Tab("test2", Root / "test2", false)))

object TabMenuSpec extends ZIOSpecDefault {
  override def spec = suite("TabMenuSpec")(
    test("it should set tab active") {
      val menu = tabMenu.copy()
      menu.setActiveTab(Root / "test2")
      assertTrue(menu.items.find(i => i.path == Root / "test2").get.active)
      assertTrue(!menu.items.find(i => i.path == Root / "test").get.active)
      menu.setActiveTab(Root / "test")
      assertTrue(!menu.items.find(i => i.path == Root / "test2").get.active)
      assertTrue(menu.items.find(i => i.path == Root / "test").get.active)
    },
  )
}

