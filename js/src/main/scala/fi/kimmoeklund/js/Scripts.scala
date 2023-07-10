package fi.kimmoeklund.js

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.Input

object Scripts {

  def main (args: Array[String]) = {
    registerEventHandler()
  }

  def registerEventHandler() = {
    document.getElementById("add-permission").addEventListener("myEvent", (e: dom.Event) => {
      println("myEvent received")
      val form = e.target.asInstanceOf[dom.HTMLFormElement]
      form.reset()
      form.elements.find({
        case _: Input => true
        case _ => false
      }).get.asInstanceOf[Input].focus()
    })
  }
}
