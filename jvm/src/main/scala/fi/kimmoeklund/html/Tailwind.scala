package fi.kimmoeklund.html

import zio.http.html.classAttr
import zio.http.html.Attributes.PartialAttribute

object Tailwind:
  val th =
    classAttr := "[&:not(:first-of-type)]:px-3" :: "py-3.5" :: "text-left" :: "text-sm" :: "font-semibold" :: "text-gray-900" :: Nil
  val td = classAttr := "whitespace-nowrap" :: "px-3" :: "py-4" :: "text-sm" :: "text-gray-500" :: Nil
  val formLabel = classAttr := "block" :: "text-sm" :: "font-medium" :: "leading-6" :: "text-gray-900" :: Nil
  val formInput =
    classAttr := "block" :: "w-full" :: "rounded-md" :: "border-0" :: "py-1.5" :: "text-gray-900" :: "shadow-sm" :: "ring-1" :: "ring-inset" :: "ring-gray-300" :: "placeholder:text-gray-400" :: "focus:ring-2" :: "focus:ring-inset" :: "focus:ring-indigo-600" :: "sm:text-sm" :: "sm:leading-6" :: Nil
