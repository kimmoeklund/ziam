package fi.kimmoeklund.html.forms

import fi.kimmoeklund.html.*
import fi.kimmoeklund.html.encoder.*
import zio.http.template.*

private def inputElement(site: String, annotation: Option[Any], attrs: Seq[Html], value: Option[String] = None) =
  (annotation, value) match {
    case (Some(_: inputEmail), _) =>
      input(
        attrs.appended(typeAttr := "email").appended(valueAttr := value.getOrElse("")): _*
      )
    case (Some(_: inputPassword), _) =>
      input(
        attrs.appended(typeAttr := "password").appended(valueAttr := value.getOrElse("")): _*
      )
    case (Some(o: inputSelectOptions), v) =>
      selectOption(
        s"/$site${o.path}",
        o.name,
        v.map(valueString => valueString.split(",").toSeq).getOrElse(Seq.empty),
        o.selectMultiple
      )
    case (Some(_: inputHidden), Some(v)) if v != "" =>
      input(attrs.appended(typeAttr := "hidden").appended(valueAttr := value.get): _*)
    case (Some(_: inputHidden), _) =>
      Dom.empty
    case (_, _) =>
      input(
        attrs.appended(typeAttr := "text").appended(valueAttr := value.getOrElse("")): _*
      )
  }

private def inputLabel(paramName: String, annotation: Option[Any], attrs: Html) =
  annotation match {
    case Some(_: inputHidden) => Dom.empty
    case Some(_)              => label(paramName, forAttr := paramName, attrs)
    case None                 => label(paramName, forAttr := paramName, attrs)
  }

private def withErrors(site: String, paramName: String, value: Option[String]) =
  inputElement(site, None, Seq(Tailwind.formInputError, nameAttr := paramName), value)

private def withErrorsAnnotated(site: String, annotation: Option[Any], paramName: String, value: Option[String]) =
  inputElement(site, annotation, Seq(Tailwind.formInputError, nameAttr := paramName), value)

private def withDefaults(site: String, annotation: Option[Any], paramName: String, value: Option[String]) =
  inputElement(site, annotation, Seq(Tailwind.formInput, nameAttr := paramName), value)

def formTemplate(site: String) = (in: TemplateInput[String]) =>
  play.twirl.api.Html(
    div(
      inputLabel(in.paramName.capitalize, in.annotations.headOption, Tailwind.formLabel),
      div(
        classAttr := "mt-2",
        // format: off
        in match
          case TemplateInput(value, Some(errors), param, Nil) => Seq(withErrors(site, param, value)) ++ errors.map(e => p(e, Tailwind.errorMsg))
          case TemplateInput(value, Some(errors), param, annotations) => Seq(withErrorsAnnotated(site, annotations.headOption, param, value)) ++ errors.map(e => p(e, Tailwind.errorMsg))
          case TemplateInput(value, None, param, annotations) => withDefaults(site, annotations.headOption, param, value)
        // format: on
      )
    ).encode.toString
  )
