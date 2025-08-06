val zioLoggingVersion     = "2.4.0"
val logbackClassicVersion = "1.4.4"
val quillVersion          = "4.8.6"
val testContainersVersion = "0.40.11"
val zioVersion            = "2.1.14"
val zioMockVersion        = "1.0.0-RC12"
ThisBuild / scalaVersion := "3.3.4"
ThisBuild / organization := "fi.kimmoeklund"

ThisBuild / assemblyMergeStrategy := {
  case x if x.endsWith("module-info.class")                          => MergeStrategy.discard
  case PathList("META-INF", "io.netty.versions.properties", xs @ _*) => MergeStrategy.concat
  case PathList("io", "getquill", xs @ _*)                           => MergeStrategy.first
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

libraryDependencies ++= Seq(
  "dev.zio"           %% "zio"               % zioVersion,
  "dev.zio"           %% "zio-json"          % "0.7.4",
  "dev.zio"           %% "zio-http"          % "3.0.1",
  "dev.zio"           %% "zio-logging"       % zioLoggingVersion,
  "io.getquill"       %% "quill-zio"         % quillVersion,
  "io.getquill"       %% "quill-jdbc-zio"    % quillVersion,
  "org.xerial"         % "sqlite-jdbc"       % "3.28.0",
  "org.postgresql"     % "postgresql"        % "42.2.8",
  "dev.zio"           %% "zio-logging"       % zioLoggingVersion,
  "dev.zio"           %% "zio-logging-slf4j" % zioLoggingVersion,
  "ch.qos.logback"     % "logback-classic"   % logbackClassicVersion,
  "com.outr"          %% "scalapass"         % "1.2.5",
  "io.github.arainko" %% "ducktape"          % "0.1.8",
  "dev.zio"           %% "zio-prelude"       % "1.0.0-RC19",
  "dev.zio"           %% "zio-cli"           % "0.5.0",
  "dev.zio"           %% "zio-test"          % zioVersion     % Test,
  "dev.zio"           %% "zio-test-sbt"      % zioVersion     % Test,
  "dev.zio"           %% "zio-test-junit"    % zioVersion     % Test,
  "dev.zio"           %% "zio-mock"          % zioMockVersion % Test,
  "dev.zio"           %% "zio-test-magnolia" % zioVersion     % Test
)
testFrameworks             := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
assembly / mainClass       := Some("fi.kimmoeklund.ziam.Main")
assembly / assemblyJarName := "ziam.jar"
Compile / selectMainClass  := Some("fi.kimmoeklund.ziam.Main")
reStart / mainClass        := Some("fi.kimmoeklund.ziam.Main")

lazy val root = (project in file(".")).enablePlugins(SbtTwirl)
