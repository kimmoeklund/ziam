package fi.kimmoeklund.repository

import fi.kimmoeklund.domain.ErrorCode

import zio.IO
import java.util.UUID
import fi.kimmoeklund.domain.ExistingEntityError
import io.getquill.jdbczio.Quill
import io.getquill.CompositeNamingStrategy2
import io.getquill.SnakeCase
import io.getquill.Escape

type QuillCtx = Quill.Sqlite[CompositeNamingStrategy2[SnakeCase, Escape]]

trait Repository[A, Id, InputForm]:
  def delete(using quill: QuillCtx)(id: Id): IO[ErrorCode, Unit]
  def update(using quill: QuillCtx)(id: Id, inputForm: InputForm): IO[ErrorCode, Option[A]]
  def getList(using quill: QuillCtx): IO[ExistingEntityError, Seq[A]]
  def getByIds(using quill: QuillCtx)(ids: Set[Id]): IO[ExistingEntityError, Set[A]]
  def add(using quill: QuillCtx)(id: Id, inputForm: InputForm): IO[ErrorCode, A]
