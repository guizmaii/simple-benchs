ThisBuild / organization := "io.simplesource"
ThisBuild / scalaVersion := "2.12.10"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val gatling = (
  (version: String) =>
    Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % version % "test,it",
      "io.gatling"            % "gatling-test-framework"    % version % "test,it"
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
    .settings(libraryDependencies ++= Seq(
      "io.simplesource" % "user" % "0.1.0-SNAPSHOT",
      "com.typesafe" % "config" % "1.4.0"
    ) ++ gatling)
