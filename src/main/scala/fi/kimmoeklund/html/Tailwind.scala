package fi.kimmoeklund.html

import zio.http.template.Attributes.PartialAttribute
import zio.http.template.classAttr

object Tailwind:

  private def formInputList(color: String = "gray", ascentColor: String = "indigo") =
    s"block w-full rounded-md border-0 py-1.5 text-${color}-900 shadow-sm ring-1 ring-inset ring-${color}-300 placeholder:text-${color}-400 focus:ring-2 focus:ring-inset focus:ring-${ascentColor}-600 sm:text-sm sm:leading-6"

  val th =
    classAttr := "[&:not(:first-of-type)]:px-3 py-3.5 text-left text-sm font-semibold text-gray-900"
  val td        = classAttr := "whitespace-nowrap px-3 py-4 text-sm text-gray-500"
  val formLabel = classAttr := "block text-sm font-medium leading-6 text-gray-900"
  val formInput =
    classAttr := formInputList()
  val formInputError = classAttr := formInputList("red", "red") + " pr-10"
  val errorMsg       = classAttr := "mt-2 text-sm text-red-600"
