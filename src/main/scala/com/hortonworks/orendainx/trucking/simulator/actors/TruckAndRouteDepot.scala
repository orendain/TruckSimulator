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

  private val trucksAvailable = mutable.Queue.empty[Truck]
  private val routesAvailable = RouteParser(config.getString("options.route-directory")).routes.toBuffer
  private val newTruckIds = Random.shuffle(1 to config.getInt("simulator.max-trucks")).toBuffer

  def receive = {
    case RequestTruck =>
      if (trucksAvailable.nonEmpty) sender() ! trucksAvailable.dequeue()
      else sender() ! newTruckIds.remove(0)

    case RequestRoute =>
      if (routesAvailable.nonEmpty) sender() ! routesAvailable.remove(0)
      else self forward RequestRoute // Requeue message for later processing

    case ReturnTruck(truck) => trucksAvailable.enqueue(truck)
    case ReturnRoute(route) => routesAvailable.append(route)
  }
}
