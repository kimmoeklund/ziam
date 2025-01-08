package fi.kimmoeklund.html.encoder

import zio.http.Form
import magnolia1.Derivation
import magnolia1.CaseClass
import magnolia1.SealedTrait
import scala.deriving.Mirror
import scala.meta.common.Convert
import scala.compiletime.erasedValue 

trait FormDecoder[A]:
  def decode(form: Form): Option[A]

object FormDecoder extends Derivation[FormDecoder]:
  import scala.compiletime.{erasedValue}
  inline def apply[A](using parser: FormDecoder[A]): FormDecoder[A] = parser

  def join[A](ctx: CaseClass[FormDecoder, A]): FormDecoder[A] = new FormDecoder[A]:
    override def decode(form: Form): Option[A] =
      val paramValues = ctx.params.map { param =>
        val filteredForm = Form(form.get(param.label).toList: _*)
        param.typeclass.decode(filteredForm)
      }
      Some(ctx.construct(param => paramValues(param.index).getOrElse(None)))

  def split[A](ctx: SealedTrait[FormDecoder, A]): FormDecoder[A] = new FormDecoder[A]:
    override def decode(form: Form): Option[A] =
      ctx.subtypes.toList.flatMap(sub => sub.typeclass.decode(form)).headOption

  inline given [A](using Mirror.Of[A]): FormDecoder[A] = derived[A]

  given FormDecoder[String] with
    def decode(form: Form): Option[String] = form.map.values.headOption.flatMap(_.stringValue)

  given FormDecoder[Int] with
    def decode(form: Form): Option[Int] = form.map.values.headOption.flatMap(_.stringValue).flatMap(_.toIntOption)

  // FormDecoder[Option[A]] givens are for the case that for case classes used for forms contain often only values
  // wrapped in Optional 

  given [A](using parser: FormDecoder[A]): FormDecoder[Option[A]] with
    def decode(form: Form): Option[Option[A]] =
      if form.map.isEmpty then None
      else parser.decode(form).map(Some(_))

  given convertBased[A](using parser: FormDecoder[String], conv: Convert[String, Option[A]]): FormDecoder[Option[A]]
    with
    def decode(form: Form): Option[Option[A]] =
      if form.map.isEmpty then None
      else parser.decode(form).map(conv(_))

  given commaSeparatedSet[A](using
      parser: FormDecoder[String],
      conv: Convert[String, Option[A]]
  ): FormDecoder[Option[Set[A]]] with
    override def decode(form: Form): Option[Option[Set[A]]] =
      if form.map.isEmpty then None
      if form.map.isEmpty then Some(Some(Set.empty))
      else
        parser.decode(form).flatMap { str =>
          if str.trim.isEmpty then Some(Some(Set.empty))
          else
            val values = str
              .split(",")
              .toList
              .flatMap(s => conv(s.trim))
            if values.isEmpty then None // Conversion failed for all values
            else Some(Some(values.toSet))
        }
