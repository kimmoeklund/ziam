package fi.kimmoeklkund.html.encoder

import scala.annotation.Annotation

final class testAnnotation extends Annotation
final class testAnnotationWithValue(val data: String) extends Annotation

enum TestEnum {
  case Foo
  case Bar(happy: String)
}

case class AnnotatedData(@testAnnotation val data: String, @testAnnotationWithValue("value") val data1: Int)
case class AnnotatedOpt(
    @testAnnotation val data: Option[String],
    @testAnnotationWithValue("value") val data1: Option[Int]
)
case class Data(val data: String, val data1: Int)
case class DataOpt(val data: Option[String])
