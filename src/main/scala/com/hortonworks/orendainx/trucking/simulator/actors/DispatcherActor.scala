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
object DispatcherActor {
  case object RequestTruck
  case object RequestRoute

  case class ReturnTruck(truck: Truck)
  case class ReturnRoute(route: Route)

  def props()(implicit config: Config) =
    Props(new DispatcherActor())
}

class DispatcherActor(implicit config: Config) extends Actor with ActorLogging {

  import DispatcherActor._

  private val trucksAvailable = mutable.Queue.empty[Truck]
  private val routesAvailable = RouteParser(config.getString("simulation.route-directory")).routes.toBuffer
  private val newTruckIds = Random.shuffle(1 to config.getInt("dispatch.max-trucks")).toBuffer

  override def receive: Unit = {
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
