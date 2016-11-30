name := "truckSimulator"

version := "0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.14",

  "com.typesafe" % "config" % "1.3.1",
  "com.github.pathikrit" %% "better-files" % "2.16.0"
)
