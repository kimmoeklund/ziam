package fi.kimmoeklund.html

import magnolia1.*
import zio.*
import zio.http.html.Html

type ElementTemplate = TemplateInput => Html

case class ErrorMsg(val paramName: String, val msg: String)

extension (errors: Option[Seq[ErrorMsg]])
  def errorMsgs(paramName: String): Option[Seq[String]] = errors.flatMap(e =>
    val result = e.filter(_.paramName == paramName)
    if result.isEmpty then None else Some(result.map(_.msg))
  )

case class TemplateInput(
    val value: Option[String],
    val errors: Option[Seq[String]],
    val paramName: String,
    val annotations: Seq[Any]
)

trait HtmlEncoder[A]:
  def encodeParams(
      template: ElementTemplate,
      paramName: String = "",
      annotations: Seq[Any] = Seq.empty
  ): List[Html]
  def encodeValues(
      template: ElementTemplate,
      value: A,
      errors: Option[Seq[ErrorMsg]] = None,
      paramName: Option[String] = None,
      annotations: Seq[Any] = Seq.empty
  ): List[Html]

object HtmlEncoder extends Derivation[HtmlEncoder] {

  inline def apply[A](using A: HtmlEncoder[A]): HtmlEncoder[A] = A

  override def split[A](ctx: SealedTrait[HtmlEncoder, A]): HtmlEncoder[A] =
    new HtmlEncoder[A]:
      def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any]) =
        val input = TemplateInput(
          None,
          None,
          ctx.typeInfo.short,
          ctx.annotations.toSeq
        )
        List(template(input))

      def encodeValues(
          template: ElementTemplate,
          value: A,
          errors: Option[Seq[ErrorMsg]],
          paramName: Option[String],
          annotations: Seq[Any]
      ): List[Html] = ctx.choose(value) { sub =>
        // use value only if it's different than the typeInfo short, as for enums without value, they are the same
        val input = TemplateInput(
          Some(if sub.typeInfo.short != value.toString then value.toString else sub.typeInfo.short),
          errors.errorMsgs(sub.typeInfo.short),
          sub.typeInfo.short,
          sub.annotations
        )
        List(template(input))
      }

  override def join[A](ctx: CaseClass[HtmlEncoder, A]): HtmlEncoder[A] =
    new HtmlEncoder[A]:
      def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any]) = {
        ctx.params.foldLeft(List[Html]()) { (acc, p) =>
          acc ++ p.typeclass.encodeParams(template, p.label, p.annotations)
        }
      }

      def encodeValues(
          template: ElementTemplate,
          value: A,
          errors: Option[Seq[ErrorMsg]],
          paramName: Option[String],
          annotations: Seq[Any]
      ): List[Html] =
        ctx.params.foldLeft(List[Html]()) { (acc, p) =>
          acc ++ p.typeclass.encodeValues(template, p.deref(value), errors, Some(p.label), p.annotations)
        }

  given [A <: String | Int | java.util.UUID]: HtmlEncoder[A] with {
    def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any]) =
      val input = TemplateInput(None, None, paramName, annotations)
      List(template(input))

    def encodeValues(
        template: ElementTemplate,
        value: A,
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ): List[Html] =
      val p = paramName.getOrElse("")
      val input = TemplateInput(Some(value.toString), errors.errorMsgs(p), p, annotations)
      List(template(input))
  }

  given [A: HtmlEncoder]: HtmlEncoder[Seq[A]] with {
    def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any]) =
      HtmlEncoder[A].encodeParams(template, paramName, annotations)
    def encodeValues(
        template: ElementTemplate,
        value: Seq[A],
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ): List[Html] = if value.isEmpty then HtmlEncoder[String].encodeValues(template, "", errors, paramName, annotations)
    else
      value.flatMap((value: A) => HtmlEncoder[A].encodeValues(template, value, errors, paramName, annotations)).toList
  }

  given [A: HtmlEncoder]: HtmlEncoder[Option[A]] with {
    def encodeParams(template: ElementTemplate, paramName: String, annotations: Seq[Any]) =
      HtmlEncoder[A].encodeParams(template, paramName, annotations)
    def encodeValues(
        template: ElementTemplate,
        value: Option[A],
        errors: Option[Seq[ErrorMsg]],
        paramName: Option[String],
        annotations: Seq[Any]
    ): List[Html] =
      if value.isDefined then HtmlEncoder[A].encodeValues(template, value.get, errors, paramName, annotations)
      else HtmlEncoder[String].encodeValues(template, "", errors, paramName, annotations)
  }

}
