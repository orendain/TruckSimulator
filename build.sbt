name := "trucking-simulator"

version := "0.2"

organization := "com.hortonworks.orendainx"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.hortonworks.orendainx" %% "trucking-shared" % "0.2",

  "com.typesafe.akka" %% "akka-actor" % "2.4.14",
  "com.typesafe" % "config" % "1.3.1",
  "com.github.pathikrit" %% "better-files" % "2.16.0"
)

scalacOptions ++= Seq("-feature")
