package com.hortonworks.orendainx.trucking.simulator.actors

import java.sql.Timestamp
import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.hortonworks.orendainx.trucking.shared.models.{TruckingEvent, TruckingEventTypes}
import com.hortonworks.orendainx.trucking.simulator.collectors.EventCollector.CollectEvent
import com.hortonworks.orendainx.trucking.simulator.models.{Driver, Location, Route, Truck}
import com.typesafe.config.Config

import scala.collection.mutable
import scala.util.Random

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object DriverActor {
  case object Drive

  case class NewRoute(route: Route)
  case class NewTruck(truck: Truck)

  def props(driver: Driver, depot: ActorRef, eventCollector: ActorRef)(implicit config: Config) =
    Props(new DriverActor(driver, depot, eventCollector))
}

class DriverActor(driver: Driver, depot: ActorRef, eventCollector: ActorRef)(implicit config: Config) extends Actor with ActorLogging {

  import DriverActor._

  var truck: Option[Truck] = None
  var route: Option[Route] = None
  var locations = List.empty[Location]
  var locationsLeft = mutable.Buffer.empty[Location]

  // TODO: config only being used for 2 options, and they're shared among all users.  Factor this out.
  val SpeedingThreshold = config.getInt("simulator.speeding-threshold")
  val MaxRouteCompletedCount = config.getInt("simulator.max-route-completed-count")

  var driveCount = 0
  var routeCompletedCount = 0

  depot ! TruckAndRouteDepot.RequestRoute
  depot ! TruckAndRouteDepot.RequestTruck

  log.debug("Changing context just because.")
  context become waitingOndepot

  def receive = {
    case _ => log.info("Should never see this message.")
  }

  def driverActive: Receive = {
    case Drive =>

      log.debug("TickDriver event processing.")

      driveCount += 1

      // TODO: received indexoutofbounds ... must be route issue not being replaced
      val currentLoc = locationsLeft.remove(0)

      val speed =
        driver.drivingPattern.minSpeed + Random.nextInt(driver.drivingPattern.maxSpeed - driver.drivingPattern.minSpeed + 1)

      val eventType =
        if (speed >= SpeedingThreshold || driveCount % driver.drivingPattern.riskFrequency == 0)
          TruckingEventTypes.AllTypes(Random.nextInt(TruckingEventTypes.AllTypes.length))
        else
          TruckingEventTypes.Normal

      // Create event and emit it
      val eventTime = new Timestamp(new Date().getTime)
      val event = TruckingEvent(eventTime, truck.get.id, driver.id, driver.name, route.get.id, route.get.name, currentLoc.latitude, currentLoc.longitude, speed, eventType)
      eventCollector ! CollectEvent(event)

      // If driver completed the route, switch trucks
      if (locationsLeft.isEmpty) {
        depot ! TruckAndRouteDepot.RequestTruck
        depot ! TruckAndRouteDepot.ReturnTruck(truck.get)

        // If route traveled too many times, switch routes
        routeCompletedCount += 1
        if (routeCompletedCount > MaxRouteCompletedCount)
          "s"
        //depot ! depotActor.RequestRoute
        // TODO: need to have route.get = None, or context will prematurely switch
        else {
          locations = locations.reverse
          locationsLeft = locations.toBuffer
        }

        log.debug("Changing context to waiting!")
        context become waitingOndepot
      }

    case _ =>
  }

  def waitingOndepot: Receive = {
    case NewTruck(newTruck) =>
      truck = Some(newTruck)
      inspectState()
      log.debug("Received new truck")
    case NewRoute(newRoute) =>
      if (route.nonEmpty) depot ! TruckAndRouteDepot.ReturnRoute(route.get)
      route = Some(newRoute)
      locations = route.get.locations
      locationsLeft = locations.toBuffer
      inspectState()
      log.debug("Received new route")
    case Drive =>
      // TODO: should not requeue because then all same timestamp, but need to make sure all events are generated for driver
      // TODO: implement exactly-n-times in coordinator.
      log.debug("Drive message while waiting on depot, ignoring.")
    case _ =>
  }

  def inspectState(): Unit = {
    log.debug("POSSIBLY changing context to normal!")
    if (truck.nonEmpty && route.nonEmpty) context become driverActive
  }
}
