package fi.kimmoeklund.html

import zio.http.html.Element.PartialElement
import magnolia1.*
import zio.http.html.Dom

extension [A: HtmlEncoder](value: A)
  def wrapWith(element: PartialElement, strModifier: String => String = a => a): List[Dom] =
    summon[HtmlEncoder[A]].wrapValueWith(element, value, strModifier)

trait HtmlEncoder[A]:
  def wrapValueWith(element: PartialElement, value: A, strModifier: String => String = a => a): List[Dom]
  def wrapParametersWith(element: PartialElement, strModifier: String => String = a => a): List[Dom]

object HtmlEncoder extends Derivation[HtmlEncoder]:

  inline def apply[A](using A: HtmlEncoder[A]): HtmlEncoder[A] = A

  override def split[A](ctx: SealedTrait[HtmlEncoder, A]): HtmlEncoder[A] =
    new HtmlEncoder[A]:
      def wrapValueWith(element: PartialElement, value: A, strModifier: String => String) = ctx.choose(value) { sub =>
        List(element(strModifier(sub.typeInfo.short)))
      }
      def wrapParametersWith(element: PartialElement, strModifier: String => String) = List(element(ctx.typeInfo.short))

  override def join[A](ctx: CaseClass[HtmlEncoder, A]): HtmlEncoder[A] =
    new HtmlEncoder[A]:
      def wrapValueWith(element: PartialElement, value: A, strModifier: String => String) =
        ctx.params.foldLeft(List[Dom]()) { (acc, p) =>
          acc ++ p.typeclass.wrapValueWith(element, p.deref(value), strModifier)
        }
      def wrapParametersWith(element: PartialElement, strModifier: String => String) =
        ctx.params.map(p => element(strModifier(p.label))).toList

  given HtmlEncoder[java.util.UUID] with {
    def wrapValueWith(element: PartialElement, value: java.util.UUID, strModifier: String => String) = List(
      element(strModifier(value.toString))
    )
    def wrapParametersWith(element: PartialElement, strModifier: String => String) = List(element(strModifier("uuid")))
  }
  given HtmlEncoder[String] with {
    def wrapValueWith(element: PartialElement, value: String, strModifier: String => String) = List(
      element(strModifier(value.toString))
    )
    def wrapParametersWith(element: PartialElement, strModifier: String => String) = List(
      element(strModifier("string"))
    )
  }
  given HtmlEncoder[Int] with {
    def wrapValueWith(element: PartialElement, value: Int, strModifier: String => String) = List(
      element(value.toString)
    )
    def wrapParametersWith(element: PartialElement, strModifier: String => String) = List(element(strModifier("int")))
  }
  given [A: HtmlEncoder]: HtmlEncoder[Seq[A]] with {
    def wrapValueWith(element: PartialElement, value: Seq[A], strModifier: String => String) =
      value.flatMap(summon[HtmlEncoder[A]].wrapValueWith(element, _, strModifier)).toList
    def wrapParametersWith(element: PartialElement, strModifier: String => String) =
      summon[HtmlEncoder[A]].wrapParametersWith(element, strModifier)
  }
