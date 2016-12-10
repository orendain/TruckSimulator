package com.hortonworks.orendainx.trucking.simulator.services

import akka.actor.ActorSystem
import better.files.{File, Scannable}
import com.hortonworks.orendainx.trucking.simulator.models.{Location, Route}
import akka.event.Logging

import scala.collection.mutable.ListBuffer

/**
  * A parser for a directory storing Route files (.route extension).
  * When parsing the base directory, RouteReader traverses recursively into directories in search of every route file.
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object RouteParser {

  // TODO: for testing, hack
  def apply(directoryPath: String)(implicit system: ActorSystem) = {
    //val path = getClass.getResource(directoryPath).getPath
    val path = s"/Users/eorendain/Documents/trucking/trucking-simulator/src/main/resources/routes/$directoryPath"
    new RouteParser(path)
  }

  // TODO: Can futurize.
  def parseFile(file: File): Route = {
    val scanner = file.newScanner
    val routeId = scanner.next[Int]
    val routeName = scanner.tillEndOfLine()
    val locations = ListBuffer[Location]()
    while (scanner.hasNext)
      locations += scanner.next[Location]

    Route(routeId, routeName, locations.toList)
  }

  // Define Scanner parser for Location
  private implicit val locationParser: Scannable[Location] = Scannable { scanner =>
    Location(scanner.next[String], scanner.next[String])
  }
}

// TODO: implicit for debugging, remove.
class RouteParser(directoryPath: String)(implicit system: ActorSystem) {

  val log = Logging(system, "RouteParser")

  log.debug(s"Path is $directoryPath")

  lazy val routes: List[Route] = {
    val directory = File(directoryPath)

    if (directory.isDirectory) {
      log.debug(s"parsing!")
      directory.listRecursively
        .filter(_.extension.contains(".route"))
        .map(RouteParser.parseFile).toList
    } else {
      log.debug(s"problem parsing!")
      List.empty[Route]
    }
  }
}
