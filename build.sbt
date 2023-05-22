scalaVersion := "3.2.0"
val zioLoggingVersion = "2.1.9"
val logbackClassicVersion = "1.4.4"
val quillVersion = "4.6.0.1"
val testContainersVersion = "0.40.11"
val zioVersion = "2.0.11"
val zioMockVersion = "1.0.0-RC8"
organization := "fi.kimmoeklund"
name := "ziam"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-json" % "0.3.0-RC11",
  "dev.zio" %% "zio-http" % "3.0.0-RC1",
  "dev.zio" %% "zio-logging" % zioLoggingVersion,
  "io.getquill" %% "quill-zio" % quillVersion,
  "io.getquill" %% "quill-jdbc-zio" % quillVersion,
  "org.postgresql" % "postgresql" % "42.2.8",
  "dev.zio" %% "zio-logging" % zioLoggingVersion,
  "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion,
  "ch.qos.logback" % "logback-classic" % logbackClassicVersion,
  "com.outr" %% "scalapass" % "1.2.5",
  "dev.zio" %% "zio-test" % zioVersion % Test,
  "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
  "dev.zio" %% "zio-test-junit" % zioVersion % Test,
  "dev.zio" %% "zio-mock" % zioMockVersion % Test,
  "com.dimafeng" %% "testcontainers-scala-postgresql" % testContainersVersion % Test,
  "dev.zio" %% "zio-test-magnolia" % zioVersion % Test
)
testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
