package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.{ErrorCode, FormError, Organization}
import fi.kimmoeklund.html.{HtmlEncoder, Page}
import zio.ZIO
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.html.*
import zio.http.{Request, html as _, *}

import java.util.UUID
import scala.util.Try
import fi.kimmoeklund.html.NewResourceForm
import fi.kimmoeklund.service.UserRepository

case class OrganizationForm(name: String)

object OrganizationForm:
  given HtmlEncoder[OrganizationForm] = HtmlEncoder.derived[OrganizationForm]

case class OrganizationsPage(path: String, db: String) extends Page[UserRepository, Organization, Organization] with NewResourceForm[OrganizationForm] {
  import FormError.*

  def listItems = for {
    repo <- ZIO.serviceAt[UserRepository](db)
    organizations <- repo.get.getOrganizations
  } yield organizations

  def mapToView = o => o

  override def post(req: Request) =
    (for {
      repo <- ZIO.serviceAt[UserRepository](db)
      form <- req.body.asURLEncodedForm.orElseFail(ValueInvalid("body", "unable to parse as form"))
      name <- ZIO.fromTry(Try(form.get("name").get.stringValue.get)).orElseFail(Missing("name"))
      o <- repo.get.addOrganization(Organization(UUID.randomUUID(), name))
    } yield (o)).map(newResourceHtml(_))

  override def delete(id: String): ZIO[Map[String, UserRepository], ErrorCode, Unit] = for {
    uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(ValueInvalid("id", "unable to parse as UUID"))
    repo <- ZIO.serviceAt[UserRepository](db)
    _ <- repo.get.deleteOrganization(uuid)
  } yield ()

  override def optionsList(selected: Option[Seq[String]] = None) = listItems.map(roles =>
    roles.map(r =>
      option(
        r.name,
        valueAttr := r.id.toString,
        if selected.isDefined && selected.get.contains(r.id.toString) then selectedAttr := "selected"
      )
    )
  )
}
