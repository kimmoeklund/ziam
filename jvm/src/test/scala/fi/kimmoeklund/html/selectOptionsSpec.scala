package fi.kimmoeklund.html

import zio.test.*
import zio.http.html.Dom

object selectOptionsSpec extends ZIOSpecDefault {
  override def spec = suite("selectOptions")(
    test("it add query parameters for hx-get") {
      val selectOptions = selectOption("path", "name", Some(Seq("1", "2"))).encode.toString()
      assertTrue(selectOptions.contains("hx-get=\"path?selected=1&selected=2\""))
    },
  )
}
