package fi.kimmoeklund.ziam

import zio._
import zio.logging.{ LogFormat, console }
import zio.http.Server
import fi.kimmoeklund.ziam.hello.HelloApp
import zio.logging.backend.SLF4J
import zio.metrics._

object MainApp extends ZIOAppDefault: 
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = 
//    Runtime.removeDefaultLoggers >>> console(LogFormat.default)
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  val requestCounter = Metric.counter("requestCounter").fromConst(0)

  def run = Server.serve(HelloApp()).provide(Server.default)
