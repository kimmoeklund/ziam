package fi.kimmoeklund.html.encoder

import fi.kimmoeklund.html.encoder.ElementTemplate
import play.twirl.api.Html
import fi.kimmoeklund.html.encoder.ErrorMsg
import scala.meta.common.Convert
import scala.deriving.Mirror
import play.twirl.api.HtmlFormat

trait ValueHtmlEncoder[A]:
  def encode(
      template: ElementTemplate[String],
      value: A,
      errors: Option[Seq[ErrorMsg]] = None
  ): List[Html]

object ValueHtmlEncoder:
  def apply[A](using encoder: ValueHtmlEncoder[A]): ValueHtmlEncoder[A] = encoder

  // Hidden implementation that delegates to InternalHtmlEncoder
  protected class Impl[A](internal: InternalHtmlEncoder[A]) extends ValueHtmlEncoder[A]:
    def encode(template: ElementTemplate[String], value: A, errors: Option[Seq[ErrorMsg]]): List[Html] =
      internal.encodeValues(template, value, errors)
  end Impl

  given convertBasedOption[A](using
      convert: Convert[A, String],
      encoder: InternalHtmlEncoder[String]
  ): ValueHtmlEncoder[Option[A]] with
    def encode(
        template: ElementTemplate[String],
        value: Option[A],
        errors: Option[Seq[ErrorMsg]] = None
    ): List[Html] = value match
      case Some(v) => encoder.encodeValues(template, convert(v), errors)
      case None    => List.empty

  given convertBased[A](using convert: Convert[A, String], encoder: InternalHtmlEncoder[String]): ValueHtmlEncoder[A]
    with
    def encode(
        template: ElementTemplate[String],
        value: A,
        errors: Option[Seq[ErrorMsg]] = None
    ): List[Html] =
      encoder.encodeValues(template, convert(value), errors)

  inline given derived[A](using Mirror.Of[A]): ValueHtmlEncoder[A] =
    new Impl(InternalHtmlEncoder.derived[A])

  given [A](using internal: InternalHtmlEncoder[Option[A]]): ValueHtmlEncoder[Option[A]] =
    new Impl(internal)
