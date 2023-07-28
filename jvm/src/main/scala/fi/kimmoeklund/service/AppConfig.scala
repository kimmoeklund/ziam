package fi.kimmoeklund.service

import zio.Config

case class DbConfig(dbLocation: String, defaultDb: String)

object DbConfig {
  def config: Config[DbConfig] =
    (Config.string("db_location").withDefault("databases") ++ Config.string("db_default").withDefault("ziam")).map {
      case (location, default) =>
        DbConfig(location, default)
    }
}
