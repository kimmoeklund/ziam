package fi.kimmoeklund.html

import zio.http.html.Element.PartialElement
import magnolia1.* 
import zio.http.html.Dom

extension [A: ZiamHtml](value: A) def wrapWith(element: PartialElement): List[Dom] = summon[ZiamHtml[A]](value, element)

trait ZiamHtml[A]:
  def apply(a: A, element: PartialElement): List[Dom]
//  def wrapValueWith(element: PartialElement): Seq[Dom]
//  def wrapParametersWith(element: PartialElement): Seq[Dom]
 
object ZiamHtml extends Derivation[ZiamHtml]:

  override def split[T](ctx: SealedTrait[fi.kimmoeklund.html.ZiamHtml, T]): ZiamHtml[T] = (a, e) => ctx.choose(a) { sub => sub.typeclass(sub.value, e) }

  override def join[T](ctx: CaseClass[fi.kimmoeklund.html.ZiamHtml, T]): ZiamHtml[T] = (a, e) =>
   ctx.params.foldLeft(List[Dom]()) { (acc, p) =>
      acc ++ p.typeclass(p.deref(a), e)
    }

  given ZiamHtml[String] = (s, e) => List(e(s))
  given ZiamHtml[Int] = (i, e) => List(e(i.toString))
  given ZiamHtml[java.util.UUID] = (u, e) => List(e(u.toString))
  given [T: ZiamHtml]: ZiamHtml[Seq[T]] = (s, e) => s.toList.flatMap(summon[ZiamHtml[T]](_,e))

extension [A: Csv](value: A) def csv: List[String] = summon[Csv[A]](value)

trait Csv[A]:
  def apply(a: A): List[String]

object Csv extends Derivation[Csv]:
  def join[A](ctx: CaseClass[Csv, A]): Csv[A] = a =>
    ctx.params.foldLeft(List[String]()) { (acc, p) =>
      acc ++ p.typeclass(p.deref(a))
    }

  def split[A](ctx: SealedTrait[Csv, A]): Csv[A] = a => ctx.choose(a) { sub => sub.typeclass(sub.value) }

  given Csv[String] = l => List(s"<td>${l}</td>")
  given Csv[Int] = i => List(s"<td>${i.toString}</td>")
  given Csv[Char] = c => List(s"<td>${c.toString}</td>")
  given [T: Csv]: Csv[Seq[T]] = _.to(List).flatMap(summon[Csv[T]](_))





//    new ZiamHtml[T]:
//      def wrapValueWith(element: PartialElement) = ctx.params.foldLeft(List[Dom]()) { (acc, p) => acc ++ element(p.deref().toString()) }
//      def wrapParametersWith(element: PartialElement) = ???
//
//  override def split[T](sealedTrait: SealedTrait[fi.kimmoeklund.html.ZiamHtml, T]): ZiamHtml[T] = ???
//    new ZiamHtml[T]:
//      def wrapValueWith(element: PartialElement) = ???
//      def wrapParametersWith(element: PartialElement) = ???


