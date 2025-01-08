package fi.kimmoeklund.html.encoder

import zio.test.ZIOSpecDefault
import zio.Scope
import zio.test.*
import zio.http.Form
import zio.http.FormField
import java.util.UUID
import zio.prelude.Newtype
import scala.meta.common.Convert
import scala.util.Try

object TestId extends Newtype[UUID]:
  given Convert[String, Option[TestId]] with 
    def apply(testId: String): Option[TestId] = 
      Try(java.util.UUID.fromString(testId)).toOption.map(TestId(_))

type TestId = TestId.Type

case class TestForm(firstName: Option[String], lastName: Option[String])
case class TestFormId(id: Option[TestId], firstName: Option[String], lastName: Option[String])
case class TestFormUUID(firstName: Option[String], lastName: Option[String], id: Option[java.util.UUID])

object FormDecoderSpec extends ZIOSpecDefault:
  override def spec: Spec[TestEnvironment & Scope, Any] = suite("FormParser")(
    test("it should parse form class with strings") {
      val form = Form(FormField.Simple("firstName", "foo"), FormField.Simple("lastName", "bar"))
      println(s"form debug: $form")
      val result = FormDecoder[TestForm].decode(form)
      assertTrue(result == Some(TestForm(Some("foo"), Some("bar"))))
    },
    test("it should parse form class with strings and uuid") {
      val id = UUID.randomUUID()
      val form = Form(FormField.Simple("firstName", "foo"), FormField.Simple("lastName", "bar"), FormField.Simple("id", id.toString))
      assertTrue(FormDecoder[TestFormUUID].decode(form) == Some(TestFormUUID(Some("foo"), Some("bar"), Some(id))))
    },
    test("it should parse form with partial fields, first None") {
      val form = Form(FormField.Simple("lastName", "bar")) 
      assertTrue(FormDecoder[TestForm].decode(form) == Some(TestForm(None, Some("bar"))))
    },
    test("it should prase form with missing newtype id") {
      val form = Form(FormField.Simple("firstName", "foo")) 
      assertTrue(FormDecoder[TestFormId].decode(form) == Some(TestFormId(None, Some("foo"), None)))
    },
    test("it should parse form with newtype id") {
      val id = UUID.randomUUID()
      val form = Form(FormField.Simple("id", id.toString), FormField.Simple("firstName", "foo"), FormField.Simple("lastName", "bar"))
      assertTrue(FormDecoder[TestFormId].decode(form) == Some(TestFormId(Some(TestId(id)), Some("foo"), Some("bar"))))
    },
    test("it should parse invalid UUID as None") {
      val id = "asdasda"
      val form = Form(FormField.Simple("id", id.toString), FormField.Simple("firstName", "foo"), FormField.Simple("lastName", "bar"))
      assertTrue(FormDecoder[TestFormId].decode(form) == Some(TestFormId(None, Some("foo"), Some("bar"))))
    },
    test("it should return instance with all fields None when none of the fields are found") {
      val form = Form(FormField.Simple("smthElse", "foo")) 
      assertTrue(FormDecoder[TestForm].decode(form) == Some(TestForm(None, None)))
    },
  )
    
  
