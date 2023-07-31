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


case class OrganizationsPage(path: String, db: String) extends Page[UserRepository] {
  import FormError.*
  
  extension (o: Organization) {
    def htmlTableRow(db: String): Dom = tr(
      PartialAttribute("hx-target") := "this",
      PartialAttribute("hx-swap") := "delete",
      td(o.name),
      td(o.id.toString),
      td(
        button(
          classAttr := "btn btn-danger" :: Nil,
          "Delete",
          PartialAttribute("hx-delete") := s"/$db/organizations/${o.id}"
        )
      )
    )

    def htmlTableRowSwap(db: String): Html =
      tBody(
        PartialAttribute("hx-swap-oob") := "beforeend:#organizations-table",
        htmlTableRow(db)
      )
  }

  private def getOrganizations = for {
    repo <- ZIO.serviceAt[UserRepository](db)
    organizations <- repo.get.getOrganizations
  } yield organizations

  override def tableList = getOrganizations.map(orgs => htmlTable(orgs))

  override def post(req: Request) =
    for {
      repo <- ZIO.serviceAt[UserRepository](db)
      form <- req.body.asURLEncodedForm.orElseFail(InputValueInvalid("body", "unable to parse as form"))
      name <- ZIO.fromTry(Try(form.get("name").get.stringValue.get)).orElseFail(MissingInput("name"))
      o <- repo.get.addOrganization(Organization(UUID.randomUUID(), name))
    } yield (o).htmlTableRowSwap(db)

  override def delete(id: String): ZIO[Map[String, UserRepository], ErrorCode, Unit] = for {
    uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(InputValueInvalid("id", "unable to parse as UUID"))
    repo <- ZIO.serviceAt[UserRepository](db)
    _ <- repo.get.deleteOrganization(uuid)
  } yield ()

  def htmlTable(args: List[Organization]): Html = {
    table(
      classAttr := "table" :: Nil,
      tHead(
        tr(
          th("Organization"),
          th("ID")
        )
      ),
      tBody(id := "organizations-table", args.map(_.htmlTableRow(db)))
    ) ++
      form(
        idAttr := "add-organization",
        PartialAttribute("hx-post") := s"/$db/organizations",
        PartialAttribute("hx-swap") := "none",
        div(
          classAttr := "mb-3" :: Nil,
          label(
            "Name",
            forAttr := "name-field",
            classAttr := "form-label" :: Nil
          ),
          input(idAttr := "name-field", nameAttr := "name", classAttr := "form-control" :: Nil, typeAttr := "text")
        ),
        button(typeAttr := "submit", classAttr := "btn" :: "btn-primary" :: Nil, "Add")
      ) ++
      script(srcAttr := "/scripts")
  }

  override def optionsList =
    getOrganizations.map(orgs => orgs.map(o => option(o.name, valueAttr := o.id.toString)))
}
