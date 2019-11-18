ThisBuild / organization := "io.simplesource"
ThisBuild / scalaVersion := "2.12.10"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val gatling = (
  (version: String) =>
    Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % version % Test,
      "io.gatling"            % "gatling-test-framework"    % version % Test
    )
  )("3.3.1")

lazy val root = (project in file("."))
  .settings(name := "simple-benchs")
  .aggregate(benchs)
  .dependsOn(benchs)

lazy val benchs =
  project
    .enablePlugins(GatlingPlugin)
    .settings(libraryDependencies ++= gatling)
