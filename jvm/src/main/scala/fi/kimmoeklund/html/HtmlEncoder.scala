package fi.kimmoeklund.html

import zio.http.html.Element.PartialElement
import magnolia1.* 
import zio.http.html.Dom

extension [A: HtmlEncoder](value: A) def wrapWith(element: PartialElement): List[Dom] = summon[HtmlEncoder[A]].wrapValueWith(element, value)

trait HtmlEncoder[A]:
  def wrapValueWith(element: PartialElement, value: A): List[Dom]
  def wrapParametersWith(element: PartialElement): List[Dom]
 
object HtmlEncoder extends AutoDerivation[HtmlEncoder]:

  inline def apply[A](using A: HtmlEncoder[A]): HtmlEncoder[A] = A

  override def split[A](ctx: SealedTrait[HtmlEncoder, A]): HtmlEncoder[A] =  
    new HtmlEncoder[A]:
      def wrapValueWith(element: PartialElement, value: A) = ctx.choose(value) { sub => List(element(sub.typeInfo.short)) } 
      def wrapParametersWith(element: PartialElement) = List(element(ctx.typeInfo.short)) 

  override def join[A](ctx: CaseClass[HtmlEncoder, A]): HtmlEncoder[A] = 
    new HtmlEncoder[A]:
      def wrapValueWith(element: PartialElement, value: A) = ctx.params.foldLeft(List[Dom]()) { (acc, p) => 
        acc ++ p.typeclass.wrapValueWith(element, p.deref(value))
      }
      def wrapParametersWith(element: PartialElement) = ctx.params.map(p => element(p.label)).toList 
      

  given HtmlEncoder[java.util.UUID] with {
    def wrapValueWith(element: PartialElement, value: java.util.UUID) = List(element(value.toString))
    def wrapParametersWith(element: PartialElement) = List(element("uuid")) 
  }
  given HtmlEncoder[String] with {
    def wrapValueWith(element: PartialElement, value: String) = List(element(value.toString))
    def wrapParametersWith(element: PartialElement) = List(element("string"))
  }
  given HtmlEncoder[Int] with {
    def wrapValueWith(element: PartialElement, value: Int) = List(element(value.toString))
    def wrapParametersWith(element: PartialElement) = List(element("int")) 
  }
  given [A: HtmlEncoder]: HtmlEncoder[Seq[A]] with {
    def wrapValueWith(element: PartialElement, value: Seq[A]) = value.flatMap(summon[HtmlEncoder[A]].wrapValueWith(element, _)).toList
    def wrapParametersWith(element: PartialElement) = summon[HtmlEncoder[A]].wrapParametersWith(element)
  }

