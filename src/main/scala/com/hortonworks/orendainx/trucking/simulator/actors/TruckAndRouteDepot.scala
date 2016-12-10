package com.hortonworks.orendainx.trucking.simulator.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.hortonworks.orendainx.trucking.simulator.models.{Route, Truck}
import com.hortonworks.orendainx.trucking.simulator.services.RouteParser
import com.typesafe.config.Config

import scala.collection.mutable
import scala.util.Random

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object TruckAndRouteDepot {
  case object RequestTruck
  case object RequestRoute

  case class ReturnTruck(truck: Truck)
  case class ReturnRoute(route: Route)

  def props()(implicit config: Config) =
    Props(new TruckAndRouteDepot())
}

class TruckAndRouteDepot(implicit config: Config) extends Actor with ActorLogging {

  import TruckAndRouteDepot._

  private val trucksAvailable = Random.shuffle(1 to config.getInt("simulator.trucks-available")).map(Truck).toBuffer
  private val routesAvailable = RouteParser(config.getString("options.route-directory")).routes.toBuffer

  log.debug("Routes and trucks ready.")

  def receive = {
    case RequestTruck =>
      if (trucksAvailable.nonEmpty) sender() ! DriverActor.NewTruck(trucksAvailable.remove(0))
      else self forward RequestTruck // No trucks to give out, requeue request

    case RequestRoute =>
      if (routesAvailable.nonEmpty) sender() ! DriverActor.NewRoute(routesAvailable.remove(0))
      else self forward RequestRoute // No route to give out, requeue request

    case ReturnTruck(truck) => trucksAvailable.append(truck)
    case ReturnRoute(route) => routesAvailable.append(route)
  }
}
