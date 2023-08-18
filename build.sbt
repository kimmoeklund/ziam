val zioLoggingVersion = "2.1.9"
val logbackClassicVersion = "1.4.4"
val quillVersion = "4.6.0.1"
val testContainersVersion = "0.40.11"
val zioVersion = "2.0.11"
val zioMockVersion = "1.0.0-RC8"
ThisBuild / scalaVersion := "3.3.0"
ThisBuild / organization := "fi.kimmoeklund"

ThisBuild / assemblyMergeStrategy := {
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case PathList("META-INF", "io.netty.versions.properties", xs@ _*) => MergeStrategy.concat
  case PathList("io", "getquill", xs@ _*) => MergeStrategy.first
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

lazy val root = project.in(file(".")).aggregate(ziam.js, ziam.jvm).settings(publish := {}, publishLocal := {})
lazy val ziam = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .jvmSettings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-json" % "0.3.0-RC11",
      "dev.zio" %% "zio-http" % "3.0.0-RC2",
      "dev.zio" %% "zio-logging" % zioLoggingVersion,
      "io.getquill" %% "quill-zio" % quillVersion,
      "io.getquill" %% "quill-jdbc-zio" % quillVersion,
      "org.xerial" % "sqlite-jdbc" % "3.28.0",
      "org.postgresql" % "postgresql" % "42.2.8",
      "dev.zio" %% "zio-logging" % zioLoggingVersion,
      "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion,
      "ch.qos.logback" % "logback-classic" % logbackClassicVersion,
      "com.outr" %% "scalapass" % "1.2.5",
      "io.github.arainko" %% "ducktape" % "0.1.8",
      "dev.zio" %% "zio-prelude" % "1.0.0-RC19",
      "dev.zio" %% "zio-cli" % "0.5.0",
//      "dev.zio" %% "zio-config" % "4.0.0-RC16",
//      "dev.zio" %% "zio-config-magnolia" % "4.0.0-RC16",
//      "dev.zio" %% "zio-config-typesafe" % "4.0.0-RC16",
//      "dev.zio" %% "zio-config-refined" % "4.0.0-RC16",
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-test-junit" % zioVersion % Test,
      "dev.zio" %% "zio-mock" % zioMockVersion % Test,
      "com.dimafeng" %% "testcontainers-scala-postgresql" % testContainersVersion % Test,
      "dev.zio" %% "zio-test-magnolia" % zioVersion % Test
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    addCompilerPlugin("com.hmemcpy" %% "zio-clippy" % "0.0.1"),
    assembly / mainClass := Some("fi.kimmoeklund.ziam.Main"),
    assembly / assemblyJarName := "ziam.jar",
    semanticdbEnabled := true
  )
  .jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.4.0",
    scalaJSUseMainModuleInitializer := true,
  )

scalacOptions += "-Yrangepos, -Xsemanticdb"
