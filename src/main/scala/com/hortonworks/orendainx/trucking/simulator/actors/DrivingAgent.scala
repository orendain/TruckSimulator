package com.hortonworks.orendainx.trucking.simulator.actors

import java.sql.Timestamp
import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import com.hortonworks.orendainx.trucking.shared.models.{TruckingEvent, TruckingEventTypes}
import com.hortonworks.orendainx.trucking.simulator.models._
import com.hortonworks.orendainx.trucking.simulator.transmitters.EventTransmitter.TransmitEvent
import com.typesafe.config.Config

import scala.util.Random

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object DrivingAgent {
  case object Drive
  case object EndTrip
  case object StartTrip

  case class NewRoute(route: Route)
  case class NewTruck(truck: Truck)

  def props(driver: Driver, depot: ActorRef, eventTransmitter: ActorRef)(implicit config: Config) =
    Props(new DrivingAgent(driver, depot, eventTransmitter))
}

class DrivingAgent(driver: Driver, depot: ActorRef, eventTransmitter: ActorRef)(implicit config: Config) extends Actor with Stash with ActorLogging {

  import DrivingAgent._

  val SpeedingThreshold = config.getInt("simulator.speeding-threshold")
  val MaxRouteCompletedCount = config.getInt("simulator.max-route-completed-count")

  // Truck and route being used, and locations this driving agent has driven to
  var truck: Truck = EmptyTruck
  var route: Route = EmptyRoute
  var locations = List.empty[Location]
  var locationsRemaining = locations.iterator

  var driveCount = 0
  var routeCompletedCount = 0

  depot ! TruckAndRouteDepot.RequestRoute(route)
  depot ! TruckAndRouteDepot.RequestTruck(truck)
  context become waitingOnDepot

  def receive = {
    case _ => log.error("This message should never be seen.")
  }

  def driverActive: Receive = {
    case Drive =>
      driveCount += 1
      log.debug(s"Driver #${driver.id} processing event #$driveCount")

      val currentLoc = locationsRemaining.next()
      val speed =
        driver.drivingPattern.minSpeed + Random.nextInt(driver.drivingPattern.maxSpeed - driver.drivingPattern.minSpeed + 1)

      val eventType =
        if (speed >= SpeedingThreshold || driveCount % driver.drivingPattern.riskFrequency == 0)
          TruckingEventTypes.AllTypes(Random.nextInt(TruckingEventTypes.AllTypes.length))
        else
          TruckingEventTypes.Normal

      // Create event and emit it
      val eventTime = new Timestamp(new Date().getTime)
      val event = TruckingEvent(eventTime, truck.id, driver.id, driver.name,
        route.id, route.name, currentLoc.latitude, currentLoc.longitude, speed, eventType)
      eventTransmitter ! TransmitEvent(event)

      // If driver completed the route, switch trucks
      if (locationsRemaining.isEmpty) {
        depot ! TruckAndRouteDepot.ReturnTruck(truck)
        depot ! TruckAndRouteDepot.RequestTruck(truck)
        truck = EmptyTruck

        // If route traveled too many times, switch routes
        routeCompletedCount += 1
        if (routeCompletedCount > MaxRouteCompletedCount) {
          depot ! TruckAndRouteDepot.ReturnRoute(route)
          depot ! TruckAndRouteDepot.RequestRoute(route)
          route = EmptyRoute
        } else {
          locations = locations.reverse
          locationsRemaining = locations.iterator
        }

        log.debug("Changing context to waitingOnDepot")
        context become waitingOnDepot
      }

      // Tell the driverCoordinator we're ready for another tick
      sender() ! DriverCoordinator.TickDriver(self)
  }

  def waitingOnDepot: Receive = {
    case NewTruck(newTruck) =>
      truck = newTruck
      considerBecome()
      log.info(s"Driver (${driver.id}, ${driver.name}) received new truck with id ${newTruck.id}")
    case NewRoute(newRoute) =>
      route = newRoute
      locations = route.locations
      locationsRemaining = locations.iterator
      routeCompletedCount = 0
      considerBecome()
      log.info(s"Driver (${driver.id}, ${driver.name}) received new route: ${newRoute.name}")
    case Drive =>
      stash()
      log.debug("Received Drive command while waiting on resources. Command stashed for later processing.")
  }

  def considerBecome(): Unit = {
    if (truck != EmptyTruck && route != EmptyRoute) {
      unstashAll()
      context become driverActive
    }
  }

  // When this actor is stopped, release resources it may still be holding onto
  override def postStop(): Unit = {
    if (truck != EmptyTruck) depot ! TruckAndRouteDepot.ReturnTruck(truck)
    if (route != EmptyRoute) depot ! TruckAndRouteDepot.ReturnRoute(route)
  }
}
