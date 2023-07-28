package fi.kimmoeklund.html.pages

import fi.kimmoeklund.domain.{ErrorCode, FormError, Organization}
import fi.kimmoeklund.html.{Effects, Renderer}
import fi.kimmoeklund.service.UserRepository
import zio.ZIO
import zio.http.html.*
import zio.http.html.Attributes.PartialAttribute
import zio.http.html.Html.fromDomElement
import zio.http.{Request, html as _, *}

import java.util.UUID
import scala.util.Try

object OrganizationsEffects extends Effects[UserRepository, Organization] with Renderer[Organization] {

  import FormError.*

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

  override def getEffect: ZIO[Map[String, UserRepository], ErrorCode, List[Organization]] =
    for {
      repo <- ZIO.serviceAt[UserRepository]("ziam")
      organizations <- repo.get.getOrganizations
    } yield organizations

  override def postEffect(req: Request): ZIO[Map[String, UserRepository], ErrorCode, Organization] =
    for {
      repo <- ZIO.serviceAt[UserRepository]("ziam")
      form <- req.body.asURLEncodedForm.orElseFail(InputValueInvalid("body", "unable to parse as form"))
      name <- ZIO.fromTry(Try(form.get("name").get.stringValue.get)).orElseFail(MissingInput("name"))
      o <- repo.get.addOrganization(Organization(UUID.randomUUID(), name))
    } yield o

  override def deleteEffect(id: String): ZIO[Map[String, UserRepository], ErrorCode, Unit] = for {
    uuid <- ZIO.attempt(UUID.fromString(id)).orElseFail(InputValueInvalid("id", "unable to parse as UUID"))
    repo <- ZIO.serviceAt[UserRepository]("ziam")
    _ <- repo.get.deleteOrganization(uuid)
  } yield ()

  override def htmlTable(args: List[Organization]): Html = {
    table(
      classAttr := "table" :: Nil,
      tHead(
        tr(
          th("Organization"),
          th("ID")
        )
      ),
      tBody(id := "organizations-table", args.map(htmlTableRow))
    ) ++
      form(
        idAttr := "add-organization",
        PartialAttribute("hx-post") := "/organizations",
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

  override def postResult(item: Organization): Html = item.htmlTableRowSwap

  override def optionsList(args: List[Organization]): Html = args.map(o => option(o.name, valueAttr := o.id.toString))
}
