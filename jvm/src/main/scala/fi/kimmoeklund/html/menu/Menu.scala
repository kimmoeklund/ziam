package fi.kimmoeklund.html.menu

import zio.http.html.Html

def menuHtml = Html.fromString("""
  <ul class="nav nav-tabs">
    <li class="nav-item">
      <a class="nav-link active" href="#">Permissions</a>
    </li>
    <li class="nav-item">
      <a class="nav-link" href="#">Roles</a>
    </li>
    <li class="nav-item">
      <a class="nav-link" href="#">Users</a>
    </li>
  </ul>""")
