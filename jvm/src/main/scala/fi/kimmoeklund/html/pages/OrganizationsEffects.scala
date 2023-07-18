package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.Organization
import fi.kimmoeklund.html.{Effects, Renderer, SimplePage, SiteMap}
import fi.kimmoeklund.service.UserRepository
import zio.ZIO
import zio.http.Request
import zio.http.{html as _, *}
import zio.http.html.{option, th, *}
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement

import java.util.UUID

object OrganizationsEffects extends Effects[UserRepository, Organization] with Renderer[Organization] {

  extension (o: Organization) {
    def htmlTableRow: Dom = tr(
      PartialAttribute("hx-target") := "this",
      PartialAttribute("hx-swap") := "delete",
      td(o.name),
      td(o.id.toString),
      td(
        button(
          classAttr := "btn btn-danger" :: Nil,
          "Delete",
          PartialAttribute("hx-delete") := "/organizations/" + o.id.toString
        )
      )
    )

    def htmlTableRowSwap: Dom =
      tBody(
        PartialAttribute("hx-swap-oob") := "beforeend:#organizations-table",
        htmlTableRow
      )
  }

  override def getEffect: ZIO[UserRepository, Throwable, List[Organization]] =
    for {
      organizations <- UserRepository.getOrganizations()
    } yield organizations

  override def postEffect(req: Request): ZIO[UserRepository, Option[Nothing] | Throwable, Organization] = for {
    form <- req.body.asURLEncodedForm
    name <- ZIO.fromOption(form.get("name").get.stringValue)
    o <- UserRepository.addOrganization(Organization(UUID.randomUUID(), name))
  } yield o

  override def deleteEffect(id: String): ZIO[UserRepository, Option[Nothing] | Throwable, Unit] = for {
      uuid <- ZIO.attempt(UUID.fromString(id))
      _ <- UserRepository.deleteOrganization(uuid)
    } yield ()

  override def htmlTable(args: List[Organization]): Html = {
    table(
      classAttr := "table" :: Nil,
      tHead(
        tr(
          th("Organization"),
          th("ID"),
        )
      ),
      tBody(id := "organizations-table", args.map(htmlTableRow))
    ) ++
      form(
        idAttr := "add-organization",
        PartialAttribute("hx-post") := "/organizations",
        PartialAttribute("hx-swap") := "none",
        div(classAttr := "mb-3" :: Nil,
          label(
            "Name",
            forAttr := "name-field",
            classAttr := "form-label" :: Nil,
          ),
          input(idAttr := "name-field", nameAttr := "name", classAttr := "form-control" :: Nil, typeAttr := "text"),
        ),
        button(typeAttr := "submit", classAttr := "btn" :: "btn-primary" :: Nil, "Add")
      ) ++
      script(srcAttr := "/scripts")
  }

  override def postResult(item: Organization): Html = item.htmlTableRowSwap

  override def optionsList(args: List[Organization]): Html = args.map(o => option(o.name, valueAttr := o.id.toString))
}

object OrganizationsPage extends SimplePage(Root / "organizations", SiteMap.tabs.setActiveTab(SiteMap.organizationsTab), OrganizationsEffects)