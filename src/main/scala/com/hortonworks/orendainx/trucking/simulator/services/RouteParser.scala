package com.hortonworks.orendainx.trucking.simulator.services

import better.files.{File, Scannable}
import com.hortonworks.orendainx.trucking.simulator.models.{Location, Route}

import scala.collection.mutable.ListBuffer

/**
  * A parser for a directory storing Route files (.route extension).
  * When parsing the base directory, RouteReader traverses recursively into directories in search of every route file.
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object RouteParser {

  def apply(routeDirectory: String) = {
    val path = s"${getClass.getResource("/routes").getPath}/$routeDirectory"
    new RouteParser(path)
  }

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

class RouteParser(directoryPath: String) {

  lazy val routes: List[Route] = {
    val directory = File(directoryPath)

    if (directory.isDirectory)
      directory.listRecursively
        .filter(_.extension.contains(".route"))
        .map(RouteParser.parseFile).toList
    else
      List.empty[Route]
  }
}
