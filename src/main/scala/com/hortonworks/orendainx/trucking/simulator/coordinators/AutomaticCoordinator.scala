package com.hortonworks.orendainx.trucking.simulator.coordinators

import akka.actor.{ActorLogging, ActorRef, PoisonPill, Props, Terminated}
import com.hortonworks.orendainx.trucking.simulator.actors.DrivingAgent
import com.hortonworks.orendainx.trucking.simulator.models.Driver
import com.typesafe.config.Config

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.Random

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object AutomaticCoordinator {
  case class TickDrivingAgent(drivingAgent: ActorRef)

  def props(drivers: Seq[Driver], depot: ActorRef, eventTransmitter: ActorRef)(implicit config: Config) =
    Props(new AutomaticCoordinator(drivers, depot, eventTransmitter))
}

class AutomaticCoordinator(drivers: Seq[Driver], depot: ActorRef, eventTransmitter: ActorRef)(implicit config: Config) extends DriverCoordinator with ActorLogging {

  // For receive messages and an execution context
  import AutomaticCoordinator._
  import DriverCoordinator._
  import context.dispatcher

  // Extract some configs
  val eventCount = config.getInt("options.event-count")
  val eventDelay = config.getInt("simulator.event-delay")
  val eventDelayJitter = config.getInt("simulator.event-delay-jitter")

  // Create new drivers and initialize a drive counter for each
  val drivingAgents = drivers.map { driver => context.actorOf(DrivingAgent.props(driver, depot, eventTransmitter)) }
  val driveCounters = mutable.Map(drivingAgents.map((_, 0)): _*)

  // Insert each new driver into the simulation (at a random scheduled point) and begin "ticking"
  drivingAgents.foreach { driverRef =>
    context.system.scheduler.scheduleOnce(Random.nextInt(eventDelay + eventDelayJitter).milliseconds, self, TickDrivingAgent(driverRef))
  }

  def receive = {
    case AcknowledgeTick(drivingAgent) =>
      self ! TickDrivingAgent(drivingAgent) // Each ack triggers another tick

    case TickDrivingAgent(drivingAgent) =>
      driveCounters.update(drivingAgent, driveCounters(drivingAgent)+1)

      if (driveCounters(drivingAgent) <= eventCount) {
        context.system.scheduler.scheduleOnce((eventDelay + Random.nextInt(eventDelayJitter)).milliseconds, drivingAgent, DrivingAgent.Drive)
      } else {
        // Kill the individual drivingAgent as we are done with it.
        drivingAgent ! PoisonPill

        // If all other drivers have met their driveCount, kill the transmitter
        if (!driveCounters.values.exists(_ <= eventCount)) {
          eventTransmitter ! PoisonPill
          context.watch(eventTransmitter)
        }
      }

    case Terminated(`eventTransmitter`) =>
      // When the eventTransmitter is killed, the system is safe to terminate
      context.system.terminate()
  }

}
