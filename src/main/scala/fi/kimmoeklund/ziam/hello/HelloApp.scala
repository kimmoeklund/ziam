package fi.kimmoeklund.ziam.hello

import zio._
import zio.http._
import zio.metrics._

object HelloApp:
  val requestCounter = Metric.counter("requestCounter").fromConst(1)

  def apply(): HttpApp[Any, Nothing] = Http.collectZIO[Request] {
    case Method.GET -> !! / "text" => for {
      _ <- ZIO.unit @@ requestCounter
      requests <- requestCounter.value
      _ <- ZIO.logDebug(s"sending hello response ${requests.count}")
    } yield Response.text("cha-cha-cha!") 
  } 
