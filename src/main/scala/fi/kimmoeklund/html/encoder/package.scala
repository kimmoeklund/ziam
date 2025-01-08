package fi.kimmoeklund.html.encoder

import scala.meta.common.Convert
import play.twirl.api.Html
import scala.util.Try

type ElementTemplate[A] = TemplateInput[A] => Html

case class ErrorMsg(val paramName: String, val msg: String)

extension (errors: Option[Seq[ErrorMsg]])
  def errorMsgs(paramName: String): Option[Seq[String]] = errors.flatMap(e =>
    val result = e.filter(_.paramName == paramName)
    if result.isEmpty then None else Some(result.map(_.msg))
  )

case class TemplateInput[A](
    val value: Option[A],
    val errors: Option[Seq[String]],
    val paramName: String,
    val annotations: Seq[Any]
)

given Convert[Int, String] with
  def apply(int: Int): String = int.toString
given Convert[java.util.UUID, String] with
  def apply(uuid: java.util.UUID): String = uuid.toString


given Convert[String, Option[java.util.UUID]] with
  def apply(str: String): Option[java.util.UUID] = Try(java.util.UUID.fromString(str)).toOption
