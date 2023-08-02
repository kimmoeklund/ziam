package fi.kimmoeklund.html

import zio.http.html.Element.PartialElement
import magnolia1.* 
import zio.http.html.Dom

extension [A: ZiamHtml](value: A) def wrapWith(element: PartialElement): List[Dom] = summon[ZiamHtml[A]].wrapValueWith(element, value)

trait ZiamHtml[A]:
  def wrapValueWith(element: PartialElement, value: A): List[Dom]
  def wrapParametersWith(element: PartialElement): List[Dom]
 
object ZiamHtml extends AutoDerivation[ZiamHtml]:

  inline def apply[A](using A: ZiamHtml[A]): ZiamHtml[A] = A

  override def split[A](ctx: SealedTrait[fi.kimmoeklund.html.ZiamHtml, A]): ZiamHtml[A] =  
    new ZiamHtml[A]:
      def wrapValueWith(element: PartialElement, value: A) = ctx.choose(value) { sub => List(element(sub.typeInfo.short)) } 
      def wrapParametersWith(element: PartialElement) = List(element(ctx.typeInfo.short)) 

  override def join[A](ctx: CaseClass[fi.kimmoeklund.html.ZiamHtml, A]): ZiamHtml[A] = 
    new ZiamHtml[A]:
      def wrapValueWith(element: PartialElement, value: A) = ctx.params.foldLeft(List[Dom]()) { (acc, p) => 
        acc ++ p.typeclass.wrapValueWith(element, p.deref(value))
      }
      def wrapParametersWith(element: PartialElement) = ctx.params.map(p => element(p.label)).toList 
      

  given ZiamHtml[java.util.UUID] with {
    def wrapValueWith(element: PartialElement, value: java.util.UUID) = List(element(value.toString))
    def wrapParametersWith(element: PartialElement) = List(element("uuid")) 
  }
  given ZiamHtml[String] with {
    def wrapValueWith(element: PartialElement, value: String) = List(element(value.toString))
    def wrapParametersWith(element: PartialElement) = List(element("string"))
  }
  given ZiamHtml[Int] with {
    def wrapValueWith(element: PartialElement, value: Int) = List(element(value.toString))
    def wrapParametersWith(element: PartialElement) = List(element("int")) 
  }
  given [A: ZiamHtml]: ZiamHtml[Seq[A]] with {
    def wrapValueWith(element: PartialElement, value: Seq[A]) = value.flatMap(summon[ZiamHtml[A]].wrapValueWith(element, _)).toList
    def wrapParametersWith(element: PartialElement) = summon[ZiamHtml[A]].wrapParametersWith(element)
  }

