package com.hortonworks.orendainx.trucking.simulator.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.hortonworks.orendainx.trucking.simulator.models.{NoTruck, Route, Truck}
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

class TruckAndRouteDepot(implicit config: Config) extends Actor with ActorLogging {

  import TruckAndRouteDepot._

  private val trucksAvailable = Random.shuffle(1 to config.getInt("simulator.trucks-available")).map(Truck).toBuffer
  private val routesAvailable = RouteParser(config.getString("options.route-directory")).routes.toBuffer

  log.info("Trucks and routes initialized and ready for deployment")

  def receive = {
    case RequestTruck(previous) if previous == NoTruck =>
      if (trucksAvailable.nonEmpty) sender() ! DrivingAgent.NewTruck(trucksAvailable.remove(0))
      else self forward RequestTruck(previous) // No truck to give out, requeue request
    case RequestTruck(previous) =>
      val ind = trucksAvailable.indexWhere(_ != previous)
      if (ind >= 0) sender() ! DrivingAgent.NewTruck(trucksAvailable.remove(ind))
      else self forward RequestTruck(previous)
    case RequestRoute(previous) =>
      if (routesAvailable.nonEmpty) sender() ! DrivingAgent.NewRoute(routesAvailable.remove(0))
      else self forward RequestRoute // No route to give out, requeue request

    case ReturnTruck(truck) => trucksAvailable.append(truck)
    case ReturnRoute(route) => routesAvailable.append(route)
  }
}
