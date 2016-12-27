package com.hortonworks.orendainx.trucking.simulator.actors

import akka.actor.{Actor, ActorLogging, Props, Stash}
import com.hortonworks.orendainx.trucking.simulator.models._
import com.hortonworks.orendainx.trucking.simulator.services.RouteParser
import com.typesafe.config.Config

import scala.util.Random

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object TruckAndRouteDepot {
  case class RequestTruck(previous: Truck)
  case class RequestRoute(previous: Route)

  case class ReturnTruck(truck: Truck)
  case class ReturnRoute(route: Route)

  def props()(implicit config: Config) =
    Props(new TruckAndRouteDepot())
}

class TruckAndRouteDepot(implicit config: Config) extends Actor with Stash with ActorLogging {

  import TruckAndRouteDepot._

  private val trucksAvailable = Random.shuffle(1 to config.getInt("simulator.trucks-available")).toList.map(Truck).toBuffer
  private val routesAvailable = RouteParser(config.getString("options.route-directory")).routes.toBuffer

  log.info("Trucks and routes initialized and ready for deployment")
  log.info(s"${trucksAvailable.length} trucks available.")
  log.info(s"${routesAvailable.length} routes available.")

  def receive = {
    case RequestTruck(previous) if previous != EmptyTruck =>
      val ind = trucksAvailable.indexWhere(_ != previous)
      if (ind >= 0) sender() ! DrivingAgent.NewTruck(trucksAvailable.remove(ind))
      else stash() // None available, stash request for later

    case RequestTruck(_) =>
      if (trucksAvailable.nonEmpty) sender() ! DrivingAgent.NewTruck(trucksAvailable.remove(0))
      else stash()

    case RequestRoute(previous) if previous != EmptyRoute =>
      val ind = routesAvailable.indexWhere(_ != previous)
      if (ind >= 0) sender() ! DrivingAgent.NewRoute(routesAvailable.remove(ind))
      else stash()

    case RequestRoute(_) =>
      if (routesAvailable.nonEmpty) sender() ! DrivingAgent.NewRoute(routesAvailable.remove(0))
      else stash()

    case ReturnTruck(truck) =>
      trucksAvailable.append(truck)
      unstashAll()

    case ReturnRoute(route) =>
      routesAvailable.append(route)
      unstashAll()
  }
}
