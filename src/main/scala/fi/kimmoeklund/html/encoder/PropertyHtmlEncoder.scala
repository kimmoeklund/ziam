package fi.kimmoeklund.html.encoder

import fi.kimmoeklund.html.encoder.ElementTemplate
import play.twirl.api.Html
import fi.kimmoeklund.html.encoder.ErrorMsg
import scala.meta.common.Convert
import scala.deriving.Mirror

trait PropertyHtmlEncoder[A]:
  def encode(
      template: ElementTemplate[String]
  ): List[Html]

object PropertyHtmlEncoder:
  def apply[A](using encoder: PropertyHtmlEncoder[A]): PropertyHtmlEncoder[A] = encoder

  // Hidden implementation that delegates to InternalHtmlEncoder
  protected class Impl[A](internal: InternalHtmlEncoder[A]) extends PropertyHtmlEncoder[A]:
    def encode(template: ElementTemplate[String]): List[Html] =
      internal.encodeParams(template)
  end Impl

  given convertBased[A](using convert: Convert[A, String], encoder: InternalHtmlEncoder[String]): PropertyHtmlEncoder[A]
    with
    def encode(
        template: ElementTemplate[String]
    ): List[Html] =
      encoder.encodeParams(template)

  given [A](using internal: InternalHtmlEncoder[Set[A]]): PropertyHtmlEncoder[Set[A]] =
    new Impl(internal)

  given [A](using internal: InternalHtmlEncoder[Option[A]]): PropertyHtmlEncoder[Option[A]] =
    new Impl(internal)

  inline given derived[A](using Mirror.Of[A]): PropertyHtmlEncoder[A] =
    new Impl(InternalHtmlEncoder.derived[A])
