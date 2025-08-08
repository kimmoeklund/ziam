package fi.kimmoeklund.domain

import fi.kimmoeklund.html.forms.BlogForm
import zio.json.*
import zio.prelude.Newtype
import java.util.UUID
import scala.meta.common.Convert
import scala.util.Try
import fi.kimmoeklund.domain.User.{given_JsonEncoder_UserId, given_JsonDecoder_UserId}

object BlogId extends Newtype[UUID]:
  given Convert[BlogId, String] with 
    def apply(blogId: BlogId): String = blogId.toString
  given Convert[String, Option[BlogId]] with 
    def apply(blogId: String): Option[BlogId] = 
      Try(java.util.UUID.fromString(blogId)).toOption.map(BlogId(_))
  def create: BlogId = BlogId(java.util.UUID.randomUUID())
type BlogId = BlogId.Type

case class Blog(id: BlogId, authorId: UserId, title: String, text: String)
    extends CrudResource[BlogForm]:
  override val form = BlogForm(
    Some(this.id),
    Some(this.authorId),
    Some(this.title),
    Some(this.text)
  )

case class NewBlog(
    id: BlogId,
    authorId: UserId,
    title: String,
    text: String
)

object Blog:
  given JsonEncoder[Blog]   = DeriveJsonEncoder.gen[Blog]
  given JsonDecoder[Blog]   = DeriveJsonDecoder.gen[Blog]
  given JsonDecoder[BlogId] = JsonDecoder[UUID].map(BlogId(_))
  given JsonEncoder[BlogId] = JsonEncoder[UUID].contramap(BlogId.unwrap)