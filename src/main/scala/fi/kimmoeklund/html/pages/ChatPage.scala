package fi.kimmoeklund.html.pages

import zio.http.Path
import fi.kimmoeklund.html.Page

case class ChatPage[R](path: Path, name: String) extends Page[R]

