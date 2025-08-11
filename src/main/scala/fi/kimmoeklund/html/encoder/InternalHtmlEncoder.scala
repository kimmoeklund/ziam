package fi.kimmoeklund.html.encoder

import magnolia1.*
import play.twirl.api.Html
import scala.deriving.Mirror
import scala.meta.common.Convert
import play.twirl.api.HtmlFormat

private[encoder] trait InternalHtmlEncoder[A]:
  def encodeParams(
      template: ElementTemplate[String],
      paramName: String = "",
      annotations: Seq[Any] = Seq.empty,
      value: Option[A] = None
  ): List[Html]
  def encodeValues(
      template: ElementTemplate[String],
      value: A,
      errors: Option[Seq[ErrorMsg]] = None,
      paramName: Option[String] = None,
      annotations: Seq[Any] = Seq.empty
  ): List[Html]

private[html] object InternalHtmlEncoder extends Derivation[InternalHtmlEncoder] with LowPriorityInternalEncoder { 
  inline def apply[A](using encoder: InternalHtmlEncoder[A]): InternalHtmlEncoder[A] = encoder

  override def split[A](ctx: SealedTrait[InternalHtmlEncoder, A]): InternalHtmlEncoder[A] =
    new InternalHtmlEncoder[A]:
      def encodeParams(template: ElementTemplate[String], paramName: String, annotations: Seq[Any], value: Option[A]) =
        val input = TemplateInput[String](
          None,
          None,
          ctx.typeInfo.short,
          ctx.annotations.toSeq
        )
        List(template(input))

      def encodeValues(
          template: ElementTemplate[String],
          value: A,
          errors: Option[Seq[ErrorMsg]],
          paramName: Option[String],
          annotations: Seq[Any]
      ): List[Html] = ctx.choose(value) { sub =>
        val input = TemplateInput[String](
          Some(if sub.typeInfo.short != value.toString then value.toString else sub.typeInfo.short),
          errors.errorMsgs(sub.typeInfo.short),
          sub.typeInfo.short,
          sub.annotations
        )
        List(template(input))
      }

  override def join[A](ctx: CaseClass[InternalHtmlEncoder, A]): InternalHtmlEncoder[A] =
    new InternalHtmlEncoder[A]:
      def encodeParams(template: ElementTemplate[String], paramName: String, annotations: Seq[Any], value: Option[A]) = {
        ctx.params.foldLeft(List[Html]()) { (acc, p) =>
          acc ++ p.typeclass.encodeParams(template, p.label, p.annotations)
        }
      }

      def encodeValues(
          template: ElementTemplate[String],
          value: A,
          errors: Option[Seq[ErrorMsg]],
          paramName: Option[String],
          annotations: Seq[Any]
      ): List[Html] =
        ctx.params.foldLeft(List[Html]()) { (acc, p) =>
          acc ++ p.typeclass.encodeValues(template, p.deref(value), errors, Some(p.label), p.annotations)
        }

  inline given [A](using Mirror.Of[A]): InternalHtmlEncoder[A] = derived

  given InternalHtmlEncoder[String] with {
    def encodeParams(template: ElementTemplate[String], paramName: String, annotations: Seq[Any], value: Option[String]) =
      val input = TemplateInput[String](None, None, paramName, annotations)
      List(template(input))

    def encodeValues(
        template: ElementTemplate[String],
        value: String,
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ): List[Html] =
      val p     = paramName.getOrElse("")
      val input = TemplateInput(Some(value.toString), errors.errorMsgs(p), p, annotations)
      List(template(input))
  }

  given InternalHtmlEncoder[Int] with {
    def encodeParams(template: ElementTemplate[String], paramName: String, annotations: Seq[Any], value: Option[Int]) =
      val input = TemplateInput[String](None, None, paramName, annotations)
      List(template(input))

    def encodeValues(
        template: ElementTemplate[String],
        value: Int,
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ): List[Html] =
      val p     = paramName.getOrElse("")
      val input = TemplateInput(Some(value.toString), errors.errorMsgs(p), p, annotations)
      List(template(input))
  }

  given InternalHtmlEncoder[java.util.UUID] with {
    def encodeParams(template: ElementTemplate[String], paramName: String, annotations: Seq[Any], value: Option[java.util.UUID]) =
      val input = TemplateInput[String](None, None, paramName, annotations)
      List(template(input))

    def encodeValues(
        template: ElementTemplate[String],
        value: java.util.UUID,
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ): List[Html] =
      val p     = paramName.getOrElse("")
      val input = TemplateInput[String](Some(value.toString), errors.errorMsgs(p), p, annotations)
      List(template(input))
  }

  given [A: InternalHtmlEncoder]: InternalHtmlEncoder[Option[A]] with {
    def encodeParams(
        template: ElementTemplate[String],
        paramName: String = "",
        annotations: Seq[Any] = Seq.empty,
        value: Option[Option[A]] = None
    ): List[Html] =
      summon[InternalHtmlEncoder[A]].encodeParams(template, paramName, annotations)

    def encodeValues(
        template: ElementTemplate[String],
        value: Option[A],
        errors: Option[Seq[ErrorMsg]] = None,
        paramName: Option[String] = None,
        annotations: Seq[Any] = Seq.empty
    ): List[Html] =
      if value.isDefined then
        summon[InternalHtmlEncoder[A]].encodeValues(
          template,
          value.get,
          errors,
          paramName,
          annotations
        )
      else
        summon[InternalHtmlEncoder[String]].encodeValues(
          template,
          "",
          errors,
          paramName,
          annotations
        )

  }
}

trait LowPriorityInternalEncoder:
  given [A](using convert: Convert[A, String], encoder: InternalHtmlEncoder[String]): InternalHtmlEncoder[A] with
    def encodeValues(
        template: ElementTemplate[String],
        value: A,
        errors: Option[Seq[ErrorMsg]] = None,
        paramName: Option[String] = None,
        annotations: Seq[Any] = Seq.empty
    ): List[Html] =
      encoder.encodeValues(template, convert(value), errors, paramName, annotations)

    def encodeParams(
        template: ElementTemplate[String],
        paramName: String,
        annotations: Seq[Any],
        value: Option[A] = None
    ): List[Html] =
      encoder.encodeParams(template, paramName, annotations, value.map(convert(_)))


