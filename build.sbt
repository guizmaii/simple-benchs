import sbt.Def

ThisBuild / organization := "io.simplesource"
ThisBuild / scalaVersion := "2.12.10"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val gatling = (
  (version: String) =>
    Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % version,
      "io.gatling"            % "gatling-test-framework"    % version
    )
  )("3.3.1")

lazy val root = (project in file("."))
  .settings(name := "simple-benchs")
  .aggregate(benchs)
  .dependsOn(benchs)

lazy val benchs =
  project
    .enablePlugins(GatlingPlugin)
    .settings(resolvers += Resolver.mavenLocal)
    .settings(disableScalacFlag("-Ywarn-dead-code"))
    .settings(
      resolvers += Resolver.mavenLocal,
      addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
      libraryDependencies ++= Seq(
        "io.simplesource"        % "simplesource-command-kafka" % "0.2.8-SNAPSHOT",
        "io.simplesource"        % "user"                       % "0.1.0-SNAPSHOT",
        "com.typesafe"           % "config"                     % "1.4.0",
      ) ++ gatling
    )

// format: off
def disableScalacFlag(flag: String): Def.SettingsDefinition = scalacOptions := scalacOptions.value.filter(_ != flag)
def disableScalacFlagInTest(flag: String): Def.SettingsDefinition = Test / scalacOptions := scalacOptions.value.filter(_ != flag)
// format: on
