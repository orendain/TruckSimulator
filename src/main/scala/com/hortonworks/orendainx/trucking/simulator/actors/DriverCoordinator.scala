package com.hortonworks.orendainx.trucking.simulator.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.hortonworks.orendainx.trucking.simulator.models.Driver
import com.typesafe.config.Config

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.Random

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object DriverCoordinator {
  case class TickDriver(driverRef: ActorRef)

  def props(drivers: Seq[Driver], depot: ActorRef, eventCollector: ActorRef)(implicit config: Config) =
    Props(new DriverCoordinator(drivers, depot, eventCollector))
}

class DriverCoordinator(drivers: Seq[Driver], depot: ActorRef, eventCollector: ActorRef)(implicit config: Config) extends Actor with ActorLogging {

  // For receive messages and an execution context
  import DriverCoordinator._
  import context.dispatcher

  // Extract some configs
  val eventCount = config.getInt("options.event-count")
  val eventDelay = config.getInt("simulator.event-delay")
  val eventDelayJitter = config.getInt("simulator.event-delay-jitter")

  // Create new drivers and initialize a counter for each
  val driverRefs = drivers.map { driver => context.actorOf(DriverActor.props(driver, depot, eventCollector)) }
  val driveCounters = mutable.Map(driverRefs.map((_, 0)): _*)

  // Insert each new driver into the simulation (at a random schedule point) and begin "ticking"
  driverRefs.foreach { driverRef =>
    context.system.scheduler.scheduleOnce(Random.nextInt(eventDelay + eventDelayJitter).milliseconds, self, TickDriver(driverRef))
  }

  def receive = {
    case TickDriver(driverRef) =>
      if (driveCounters(driverRef) < eventCount) {
        driverRef ! DriverActor.Drive
        driveCounters.update(driverRef, driveCounters(driverRef)+1)
        context.system.scheduler.scheduleOnce((eventDelay + Random.nextInt(eventDelayJitter)).milliseconds, self, TickDriver(driverRef))
      } else {
        // If a driver has finished their route, and all other drivers have, terminate the simulation.
        if (!driveCounters.values.exists(_ < eventCount)) context.system.terminate()
      }
  }

  override def postStop() = {
    log.info("DriverCoordinator stopped.")
  }

}
