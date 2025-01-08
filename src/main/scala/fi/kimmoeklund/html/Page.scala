package fi.kimmoeklund.html

import fi.kimmoeklund.html.encoder.*
import fi.kimmoeklund.repository.{QuillCtx, Repository}
import fi.kimmoeklund.service.*
import zio.http.Request
import zio.{IO, URIO, ZIO}

import scala.annotation.Annotation
import scala.reflect.TypeTest
import zio.Chunk
import zio.http.Path
import fi.kimmoeklund.domain.ExistingEntityError
import play.twirl.api.Html

final class inputEmail    extends Annotation
final class inputNumber   extends Annotation
final class inputPassword extends Annotation
final class inputHidden   extends Annotation
final class inputSelectOptions(val path: String, val name: String, val selectMultiple: Boolean = false)
    extends Annotation

trait Page[R]:
  val path: Path 
  val name: String
end Page
