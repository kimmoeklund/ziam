package fi.kimmoeklund.html

import zio.test.*
import zio.test.Assertion.*
import zio.http.Form
import zio.*
import fi.kimmoeklund.domain.FormError
import scala.concurrent.java8.FuturesConvertersImpl.P

object zioFromFieldSpec extends ZIOSpecDefault:
  val form = Form.fromStrings("foo" -> "bar")
  override def spec = suite("zioFromField")(
      test("should return Missing when field is missing") {
        for {
          result <- form.zioFromField("foz").flip
        } yield assertTrue(result == FormError.Missing("foz"))
      },
      test("should return valid value") {
        val form = Form.fromStrings("foo" -> "bar")
        for {
          result <- form.zioFromField("foo")
        } yield assertTrue(result == "bar")
      }
  )

