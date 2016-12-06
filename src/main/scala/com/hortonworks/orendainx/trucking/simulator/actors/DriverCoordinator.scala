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

import scala.concurrent.duration._

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object DriverCoordinator {
  case class TickDriver(driverRef: ActorRef)

  def props(drivers: Seq[Driver], dispatcher: ActorRef, eventCollector: ActorRef)(implicit config: Config) =
    Props(new DriverCoordinator(drivers, dispatcher, eventCollector))
}

class DriverCoordinator(drivers: Seq[Driver], dispatcher: ActorRef, eventCollector: ActorRef)(implicit config: Config) extends Actor with ActorLogging {

  import DriverCoordinator._

  // Extract some configs
  val eventCount = config.getInt("options.event-count")
  val eventDelay = config.getInt("simulator.event-delay")
  val eventDelayJitter = config.getInt("simulator.event-delay-jitter")

  // Insert each new driver into the simulation, and tell them to "Drive" after every short delay
  val driverRefs = drivers.map { driver => context.actorOf(DriverActor.props(driver, dispatcher, eventCollector)) }
  val driveCounters = mutable.Map(driverRefs.zip(Seq.fill(driverRefs.length)(0)): _*)

  // Start each driver
  driverRefs.foreach { driverRef =>
    context.system.scheduler.scheduleOnce(Random.nextInt(eventDelay + eventDelayJitter) milliseconds, self, TickDriver(driverRef))
  }

  def receive = {
    case TickDriver(driverRef) =>
      if (driveCounters(driverRef) < eventCount) {
        driverRef ! DriverActor.Drive
        driveCounters.update(driverRef, driveCounters(driverRef)-1)
        context.system.scheduler.scheduleOnce(eventDelay + Random.nextInt(eventDelayJitter) milliseconds, self, TickDriver(driverRef))
      }
    case _ => log.info("Received an unexpected message.")
  }

}
