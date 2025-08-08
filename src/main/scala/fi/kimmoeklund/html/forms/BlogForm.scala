package fi.kimmoeklund.html.forms

import fi.kimmoeklund.domain.{BlogId, UserId, FormError}
import fi.kimmoeklund.html.*
import zio.prelude.Validation
import fi.kimmoeklund.domain.FormError.*

case class BlogForm(
    @inputHidden id: Option[BlogId],
    @inputSelectOptions(s"/page/users/options", "author", false)
    author: Option[UserId],
    title: Option[String],
    text: Option[String]
)

sealed trait ValidBlogForm:
  val id: BlogId
  val author: UserId
  val title: String
  val text: String

case class ValidNewBlogForm(
    id: BlogId,
    author: UserId,
    title: String,
    text: String
) extends ValidBlogForm:
  private def copy: Unit = ()

case class ValidUpdateBlogForm(
    id: BlogId,
    author: UserId,
    title: String,
    text: String
) extends ValidBlogForm:
  private def copy: Unit = ()

object BlogFormValidators:
  def newBlog(form: BlogForm): Validation[FormError, ValidNewBlogForm] =
    Validation.validateWith(
      Validation.succeed(BlogId.create),
      Validation.fromOptionWith(Missing("author"))(form.author),
      Validation.fromOptionWith(Missing("title"))(form.title),
      Validation.fromOptionWith(Missing("text"))(form.text)
    )(ValidNewBlogForm.apply)

  def updateBlog(form: BlogForm): Validation[FormError, ValidUpdateBlogForm] =
    Validation.validateWith(
      Validation.fromOptionWith(Missing("id"))(form.id),
      Validation.fromOptionWith(Missing("author"))(form.author),
      Validation.fromOptionWith(Missing("title"))(form.title),
      Validation.fromOptionWith(Missing("text"))(form.text)
    )(ValidUpdateBlogForm.apply)