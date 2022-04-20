ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "osprey-server-scala"
  )

libraryDependencies += "org.json" % "json" % "20220320"
libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.36.0.3"