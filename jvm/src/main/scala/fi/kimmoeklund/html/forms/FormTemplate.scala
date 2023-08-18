package fi.kimmoeklund.html.forms

import fi.kimmoeklund.html.*
import zio.http.html.*

private def inputElement(annotation: Option[Any], attrs: Seq[Html], value: Option[String] = None) =
  annotation match {
    case Some(_: inputEmail) =>
      input(
        attrs.appended(typeAttr := "email").appended(valueAttr := value.getOrElse("")): _*
      )
    case Some(_: inputPassword) =>
      input(
        attrs.appended(typeAttr := "password").appended(valueAttr := value.getOrElse("")): _*
      )
    case Some(o: inputSelectOptions) =>
      value match {
        case Some(v) =>
          selectOption(s"${o.path}", o.name, Some(v.split(",").toSeq), o.selectMultiple)
        case None => 
          selectOption(s"${o.path}", o.name, None, o.selectMultiple)
      }
    case _ =>
      input(
        attrs.appended(typeAttr := "text").appended(valueAttr := value.getOrElse("")): _*
      )
  }

private def withErrors(paramName: String, value: Option[String]) =
  inputElement(None, Seq(Tailwind.formInputError, nameAttr := paramName), value)

private def withErrorsAnnotated(annotation: Any, paramName: String, value: Option[String]) =
  inputElement(Some(annotation), Seq(Tailwind.formInputError, nameAttr := paramName), value)

private def withDefaults(paramName: String, value: Option[String]) =
  inputElement(None, Seq(Tailwind.formInput, nameAttr := paramName), value)

private def withDefaultsAnnotated(annotation: Any, paramName: String, value: Option[String]) =
  inputElement(Some(annotation), Seq(Tailwind.formInput, nameAttr := paramName), value)

def formTemplate = (in: TemplateInput) =>
  Html.fromDomElement(
    div(
      label(in.paramName.capitalize, forAttr := in.paramName, Tailwind.formLabel),
      div(
        classAttr := "mt-2" :: Nil,
        // format: off
        in match
          case TemplateInput(value, Some(errors), param, Nil) => Seq(withErrors(param, value)) ++ errors.map(e => p(e, Tailwind.errorMsg))
          case TemplateInput(value, Some(errors), param, annotations) => annotations.map(withErrorsAnnotated(_, param, value)) ++ errors.map(e => p(e, Tailwind.errorMsg))
          case TemplateInput(value, None, param, Nil) => withDefaults(param, value)
          case TemplateInput(value, None, param, annotations) => 
            annotations.map(withDefaultsAnnotated(_, param, value))
        // format: on
      )
    )
  )
