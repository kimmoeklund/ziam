package fi.kimmoeklund.repository

import fi.kimmoeklund.domain.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import io.github.arainko.ducktape.*
import zio.*
import MappedEncodings.given

import java.sql.SQLException
import java.util.UUID

type BlogRepository = Repository[Blog, BlogId]

final class BlogRepositoryLive extends BlogRepository:

  def add(using quill: QuillCtx)(blog: NewBlog): IO[InsertDataError, Blog] = {
    import quill.*
    val blogRecord = blog.to[Blogs]
    run(query[Blogs].insertValue(lift(blogRecord)))
      .map(_ => Blog(blog.id, blog.authorId, blog.title, blog.text))
      .tapError({
        case e: SQLException =>
          ZIO.logError(
            s"SQLException : ${e.getMessage()} : error code: ${e.getErrorCode()}, : sqlState: ${e.getSQLState()}"
          )
        case t: Throwable => ZIO.logError(s"Throwable : ${t.getMessage()} : class: ${t.getClass().toString()}")
      })
      .mapError({
        case e: SQLException if e.getErrorCode == 19 =>
          InsertDataError.UniqueKeyViolation("id")
        case t: Throwable => InsertDataError.Exception(t.getMessage)
      })
  }



  override def delete(using quill: QuillCtx)(id: BlogId): IO[ErrorCode, Unit] = {
    import quill.*
    run {
      query[Blogs].filter(b => b.id == lift(id)).delete
    }.mapBoth(e => GeneralError.Exception(e.getMessage), _ => ())
  }

  override def update(using quill: QuillCtx)(blog: Blog): IO[ErrorCode, Option[Blog]] = {
    import quill.*
    val blogRecord = blog.to[Blogs]
    run(query[Blogs].filter(b => b.id == lift(blog.id)).updateValue(lift(blogRecord)))
      .map(_ => Some(blog))
      .mapError(e => GeneralError.Exception(e.getMessage))
  }

  override def getList(using quill: QuillCtx): IO[ExistingEntityError, Seq[Blog]] = {
    import quill.*
    run(query[Blogs])
      .map(blogs => blogs.map(b => Blog(b.id, b.authorId, b.title, b.text)))
      .mapError(e => ExistingEntityError.Exception(e.getMessage))
  }

  override def getByIds(using quill: QuillCtx)(ids: Set[BlogId]): IO[ExistingEntityError, Set[Blog]] = {
    import quill.*
    run {
      query[Blogs].filter(b => liftQuery(ids).contains(b.id))
    }
    .map(blogs => blogs.map(b => Blog(b.id, b.authorId, b.title, b.text)).toSet)
    .mapError(e => ExistingEntityError.Exception(e.getMessage))
  }

  override def add(using quill: QuillCtx)(blog: Blog): IO[ErrorCode, Blog] = {
    import quill.*
    val blogRecord = blog.to[Blogs]
    run(query[Blogs].insertValue(lift(blogRecord)))
      .map(_ => blog)
      .mapError(e => GeneralError.Exception(e.getMessage))
  }

end BlogRepositoryLive