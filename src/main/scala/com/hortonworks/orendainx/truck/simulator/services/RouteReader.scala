package com.hortonworks.orendainx.truck.simulator.services

import better.files.{File, Scannable}
import com.hortonworks.orendainx.truck.simulator.models.{Location, Route}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/**
  * A parser for a directory storing Route files (.route extension).
  * When parsing the base directory, RouteReader traverses recursive directories in search of every route files.
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
class RouteReader(directoryPath: String) {

  implicit val locationParser: Scannable[Location] = Scannable { scanner =>
    Location(scanner.next[String], scanner.next[String])
  }

  // TODO: type selection appropriate? concurrent access may be tricky
  val routes: mutable.Buffer[Route] = ArrayBuffer.empty[Route]

  def parseRoutes(): Unit = {
    val directory = File(directoryPath)

    if (directory.isDirectory) {
      directory.listRecursively
        .filter(_.extension.contains(".route"))
        .map(parseRouteFile).foreach(
          _.onSuccess { case r: Route => routes += r }
        )
    }
  }

  def parseRouteFile(file: File): Future[Route] = Future {
    val scanner = file.newScanner
    val routeId = scanner.next[Int]
    val routeName = scanner.tillEndOfLine()
    val locations = for (loc <- scanner.next[Location]) yield loc

    Route(routeId, routeName, locations)
  }
}
