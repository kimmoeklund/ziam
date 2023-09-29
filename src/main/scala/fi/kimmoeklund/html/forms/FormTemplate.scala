package fi.kimmoeklund.html.forms

import fi.kimmoeklund.html.*
import zio.http.html.*

private def inputElement(annotation: Option[Any], attrs: Seq[Html], value: Option[String] = None) =
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
      selectOption(s"${o.path}", o.name, v.map(valueString => valueString.split(",")), o.selectMultiple)
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
    case Some(_:inputHidden) => Dom.empty
    case Some(_) => label(paramName, forAttr := paramName, attrs)
    case None    => label(paramName, forAttr := paramName, attrs)
  }

private def withErrors(paramName: String, value: Option[String]) =
  inputElement(None, Seq(Tailwind.formInputError, nameAttr := paramName), value)

private def withErrorsAnnotated(annotation: Option[Any], paramName: String, value: Option[String]) =
  inputElement(annotation, Seq(Tailwind.formInputError, nameAttr := paramName), value)

private def withDefaults(annotation: Option[Any], paramName: String, value: Option[String]) =
  inputElement(annotation, Seq(Tailwind.formInput, nameAttr := paramName), value)

def formTemplate = (in: TemplateInput) =>
  Html.fromDomElement(
    div(
      inputLabel(in.paramName.capitalize, in.annotations.headOption, Tailwind.formLabel),
      div(
        classAttr := "mt-2" :: Nil,
        // format: off
        in match
          case TemplateInput(value, Some(errors), param, Nil) => Seq(withErrors(param, value)) ++ errors.map(e => p(e, Tailwind.errorMsg))
          case TemplateInput(value, Some(errors), param, annotations) => Seq(withErrorsAnnotated(annotations.headOption, param, value)) ++ errors.map(e => p(e, Tailwind.errorMsg))
          case TemplateInput(value, None, param, annotations) => withDefaults(annotations.headOption, param, value)
        // format: on
      )
    )
  )
