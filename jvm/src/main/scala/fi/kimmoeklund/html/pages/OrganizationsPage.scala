package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.{ErrorCode, FormError, Organization}
import fi.kimmoeklund.html.Page
import fi.kimmoeklund.service.UserRepository
import zio.ZIO
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.{Request, html as _, *}

import java.util.UUID
import scala.util.Try
import fi.kimmoeklund.html.HtmlEncoder

case class OrganizationForm(name: String)

case class OrganizationsPage(path: String, db: String) extends Page[UserRepository, Organization, Organization] {
  import FormError.*

  def listItems = for {
    repo <- ZIO.serviceAt[UserRepository](db)
    organizations <- repo.get.getOrganizations
  } yield organizations


  def mapToView = o => o

  override def post(req: Request) =
    (for {
      repo <- ZIO.serviceAt[UserRepository](db)
      form <- req.body.asURLEncodedForm.orElseFail(InputValueInvalid("body", "unable to parse as form"))
      name <- ZIO.fromTry(Try(form.get("name").get.stringValue.get)).orElseFail(MissingInput("name"))
      o <- repo.get.addOrganization(Organization(UUID.randomUUID(), name))
    } yield (o)).map(newResourceHtml(_))

  override def delete(id: String): ZIO[Map[String, UserRepository], ErrorCode, Unit] = for {
    uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(InputValueInvalid("id", "unable to parse as UUID"))
    repo <- ZIO.serviceAt[UserRepository](db)
    _ <- repo.get.deleteOrganization(uuid)
  } yield ()

  override def optionsList =
    listItems.map(orgs => orgs.map(o => option(o.name, valueAttr := o.id.toString)))
}
