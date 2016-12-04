package com.hortonworks.orendainx.trucking.simulator.actors

import java.sql.Timestamp
import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.hortonworks.orendainx.trucking.shared.models.TruckingEvent
import com.hortonworks.orendainx.trucking.simulator.collectors.EventCollector.CollectEvent
import com.hortonworks.orendainx.trucking.simulator.models.{Driver, Location, Route, Truck}
import com.typesafe.config.Config

import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object DriverActor {
  case object Drive

  case class NewRoute(route: Route)
  case class NewTruck(truck: Truck)

  def props(driver: Driver)(implicit dispatcher: ActorRef, config: Config, eventCollector: ActorRef) =
    Props(new DriverActor(driver))
}

class DriverActor(driver: Driver)(implicit dispatcher: ActorRef, config: Config, eventCollector: ActorRef) extends Actor with ActorLogging {

  import DriverActor._

  var truck: Option[Truck] = None
  var route: Option[Route] = None
  var locations = ListBuffer.empty[Location]

  val SpeedingThreshold = config.getInt("driving.speeding-threshold")
  val MaxRouteCompletedCount = config.getInt("driving.max-route-completed-count")

  var driveCount = 0
  var routeCompletedCount = 0

  dispatcher ! DispatcherActor.RequestRoute
  dispatcher ! DispatcherActor.RequestTruck
  context become waitingOnDispatcher

  override def receive: Unit = {
    log.info("Should never see this message.")
  }

  def driverActive: Receive = {
    case Drive =>

      driveCount += 1
      val currentLoc = locations.remove(0)

      val speed =
        driver.drivingPattern.minSpeed + Random.nextInt(driver.drivingPattern.maxSpeed - driver.drivingPattern.minSpeed + 1)

      val eventType =
        if (speed >= SpeedingThreshold || driveCount % driver.drivingPattern.riskFrequency == 0) {
        // TODO: Speed or random violation
          "speeding"
      } else {
        "normal"
      }

      // Create event and emit it
      val eventTime = new Timestamp(new Date().getTime)
      val event = TruckingEvent(eventTime, truck.get.id, driver.id, driver.name, route.get.id, route.get.name, currentLoc.latitude, currentLoc.longitude, speed, eventType)
      eventCollector ! CollectEvent(event)

      // If driver completed the route, switch trucks
      if (locations.isEmpty) {
        dispatcher ! DispatcherActor.RequestTruck
        dispatcher ! DispatcherActor.ReturnTruck(truck.get)

        // If route traveled too many times, switch routes
        routeCompletedCount += 1
        if (routeCompletedCount > MaxRouteCompletedCount)
          dispatcher ! DispatcherActor.RequestRoute
          // TODO: need to have route.get = None, or context will prematurely swtich
        else {
          // TODO: If using same route, get new set of locations from route, but in reverse
        }

        context become waitingOnDispatcher
      }

    case _ =>
  }

  def waitingOnDispatcher: Receive = {
    case NewTruck(newTruck) =>
      truck = Some(newTruck)
      inspectState()
    case NewRoute(newRoute) =>
      if (route.nonEmpty) dispatcher ! DispatcherActor.ReturnRoute(route.get)
      route = Some(newRoute)
      inspectState()
    case Drive =>
      log.info("Drive message while waiting on dispatcher, ignoring.")
    case _ =>
  }

  def inspectState(): Unit =
    if (truck.nonEmpty && route.nonEmpty) context become driverActive
}
