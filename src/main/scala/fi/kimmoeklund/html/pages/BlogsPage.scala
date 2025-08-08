package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.*
import fi.kimmoeklund.domain.FormError.ValueInvalid
import fi.kimmoeklund.html.*
import fi.kimmoeklund.html.encoder.*
import fi.kimmoeklund.html.forms.*
import fi.kimmoeklund.repository.*
import io.github.arainko.ducktape.*
import fi.kimmoeklund.html.{UpsertResult, CreatedEntity, UpdatedEntity}
import zio.http.Request
import zio.http.template.*
import zio.{Cause, Chunk, ZIO}

import java.util.UUID
import scala.annotation.threadUnsafe

import play.twirl.api.HtmlFormat
import zio.http.Path

case class BlogView(id: BlogId, authorId: UserId, title: String, text: String) extends Identifiable

object BlogView:
  def from(b: Blog) =
    BlogView(b.id, b.authorId, b.title, b.text)

case class BlogsPage(path: Path, db: String, name: String)
    extends CrudPage[BlogRepository, Blog, BlogView, BlogForm]:
  override val formValueEncoder                                   = summon[ValueHtmlEncoder[BlogForm]]
  override val formPropertyEncoder                                = summon[PropertyHtmlEncoder[BlogForm]]
  override val viewValueEncoder: ValueHtmlEncoder[BlogView]       = summon[ValueHtmlEncoder[BlogView]]
  override val viewPropertyEncoder: PropertyHtmlEncoder[BlogView] = summon[PropertyHtmlEncoder[BlogView]]
  override val errorHandler                                       = DefaultErrorHandler(name).handle;

  def mapToView = r => BlogView.from(r)

  def createOrUpdate(using
      QuillCtx
  )(form: ValidBlogForm, blogRepo: BlogRepository): ZIO[Any, ErrorCode, UpsertResult[Blog]] = form match
    case ValidNewBlogForm(id, authorId, title, text) =>
      blogRepo
        .add(Blog(id, authorId, title, text))
        .map(CreatedEntity(_))
    case ValidUpdateBlogForm(id, authorId, title, text) =>
      blogRepo
        .update(Blog(id, authorId, title, text))
        .map(blog => UpdatedEntity(blog.getOrElse(Blog(id, authorId, title, text))))

  def upsertResource(using QuillCtx)(req: Request) = {
    val parsedForm = parseForm(req)
    parsedForm
      .flatMap(form => {
        (for {
          blogRepo <- ZIO.service[BlogRepository]
          validForm <-
            if form.id.isEmpty then BlogFormValidators.newBlog(form).toZIO else BlogFormValidators.updateBlog(form).toZIO
          blog <- createOrUpdate(validForm, blogRepo)
        } yield blog).mapErrorCause(e => Cause.fail(FormWithErrors(e.failures, Some(form))))
      })
  }

  def deleteInternal(using QuillCtx)(id: String) =
    (for {
      blogId <- ZIO.attempt(BlogId(UUID.fromString(id))).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
      repo   <- ZIO.service[BlogRepository]
      _      <- repo.delete(blogId)
    } yield ()).mapError(errorHandler(_))

  def get(using QuillCtx)(id: String) = (for {
    blogId  <- ZIO.attempt(BlogId(UUID.fromString(id))).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
    repo    <- ZIO.service[BlogRepository]
    blogs   <- repo.getByIds(Set(blogId))
    blog    <- ZIO.fromOption(blogs.headOption).orElseFail(ExistingEntityError.EntityNotFound(id))
  } yield (blog)).mapError(errorHandler(_))

  override def renderAsOptions(using QuillCtx)(selected: Chunk[String] = Chunk.empty) =
    listItems
      .map(blogs =>
        Html.fromSeq(
          blogs.map(blog =>
            option(
              blog.title,
              valueAttr := blog.id.toString,
              if selected.contains(BlogId.unwrap(blog.id).toString) then selectedAttr := "true"
              else emptyHtml
            )
          )
        )
      )
      .map(h => HtmlFormat.raw(h.encode.toString))
      .mapError(errorHandler(_))

  def listItems(using QuillCtx) = for {
    repo  <- ZIO.service[BlogRepository]
    blogs <- repo.getList
  } yield blogs