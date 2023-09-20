package fi.kimmoeklund.service

import zio.Config
import fi.kimmoeklund.html.pages.CookieSecret

case class DbConfig(dbLocation: String, defaultDb: String)

object DbConfig {
  def config: Config[DbConfig] =
    (Config.string("db_location").withDefault("databases") ++ Config.string("db_default").withDefault("ziam")).map {
      case (location, default) =>
        DbConfig(location, default)
    }
}

object Secrets {
  def cookieSecret: Config[CookieSecret] =
    Config.string("cookie_secret").withDefault("changeme").map(CookieSecret(_))
} 

