package fi.kimmoeklund.html

import zio.http.html.Element.PartialElement
import magnolia1.*
import zio.http.html.classAttr
import zio.http.html.typeAttr
import zio.http.html.Attributes.PartialAttribute
import zio.*
import zio.http.html.Html
import scala.annotation.Annotation

//extension [A: HtmlEncoder](value: A)
//  def encodeValues(template: String => Html): List[Html] =
//    HtmlEncoder[A].encodeValues(template)
//
trait HtmlEncoder[A]:
  def encodeParams(template: (String, Seq[Html]) => Html, annotationMapper: (Any, String) => Html = (_, _) => Html.fromUnit(())) : List[Html]
  def encodeValues(template: String => Html, value: A): List[Html]

object HtmlEncoder extends Derivation[HtmlEncoder] {

  inline def apply[A](using A: HtmlEncoder[A]): HtmlEncoder[A] = A

  override def split[A](ctx: SealedTrait[HtmlEncoder, A]): HtmlEncoder[A] =
    new HtmlEncoder[A]:
      def encodeParams(template: (String, Seq[Html]) => Html, annotationMapper: (Any, String) => Html) = List(
        template(ctx.typeInfo.short, ctx.annotations.map(a => annotationMapper(a, ctx.typeInfo.short)).toSeq)) //, ctx.annotations.map(annotationMapper):_*)

      def encodeValues(template: String => Html, value: A) = ctx.choose(value) { sub =>
        // use value only if it's different than the typeInfo short, as for enums without value, they are the same
        List(template(if sub.typeInfo.short != value.toString then value.toString else sub.typeInfo.short))
      }

  override def join[A](ctx: CaseClass[HtmlEncoder, A]): HtmlEncoder[A] =
    new HtmlEncoder[A]:
      def encodeParams(template: (String, Seq[Html]) => Html, annotationMapper: (Any, String) => Html) = {
//        println("debug" + ctx.params.map(_.annotations.map((a2: Any) => a2.map(_.toString))))
        ctx.params.map(p => {
          println(p)          
          template(p.label, p.annotations.map(a => { 
            println(a)
            annotationMapper(a, p.label)
          }))
        }).toList     
      }
      
      def encodeValues(template: String => Html, value: A) = ctx.params.foldLeft(List[Html]()) { (acc, p) =>
        acc ++ p.typeclass.encodeValues(template, p.deref(value))
      }

  given [A <: String | Int | java.util.UUID]: HtmlEncoder[A] with {
    def encodeParams(template: (String, Seq[Html]) => Html, annotationMapper: (Any, String) => Html) = List(template("", Seq()))
    def encodeValues(template: String => Html, value: A): List[Html] = List(template(value.toString))
  }
  
  given [A: HtmlEncoder]: HtmlEncoder[Seq[A]] with {
    def encodeParams(template: (String, Seq[Html]) => Html, annotationMapper: (Any, String) => Html) = HtmlEncoder[A].encodeParams(template, annotationMapper)
    def encodeValues(template: String => Html, value: Seq[A]) = value.flatMap(HtmlEncoder[A].encodeValues(template, _)).toList
  }
}

