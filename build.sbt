name := "trucking-simulator"

version := "0.3"

organization := "com.hortonworks.orendainx"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.hortonworks.orendainx" %% "trucking-shared" % "0.3",

  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "ch.qos.logback" % "logback-classic" % "1.1.8",

  "com.typesafe.akka" %% "akka-actor" % "2.4.14",
  "com.typesafe" % "config" % "1.3.1",
  "com.github.pathikrit" %% "better-files" % "2.16.0"
)

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

scalacOptions ++= Seq("-feature")
