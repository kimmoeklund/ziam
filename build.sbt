scalaVersion := "3.2.0"
val zioLoggingVersion = "2.1.9"
val logbackClassicVersion = "1.4.4"
val quillVersion = "4.3.0"
organization := "fi.kimmoeklund"
name := "ziam"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "2.0.8",
  "dev.zio" %% "zio-json" % "0.3.0-RC11",
  "dev.zio" %% "zio-http" % "0.0.4",
  "dev.zio" %% "zio-logging" % zioLoggingVersion,
  "io.getquill" %% "quill-zio" % quillVersion,
  "io.getquill" %% "quill-jdbc-zio" % quillVersion,
  "org.postgresql" % "postgresql" % "42.2.8",
  "dev.zio" %% "zio-logging" % zioLoggingVersion,
  "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion,
  "ch.qos.logback" % "logback-classic" % logbackClassicVersion
)
