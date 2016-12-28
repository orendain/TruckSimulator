package com.hortonworks.orendainx.trucking.simulator.coordinators

import akka.actor.{ActorLogging, ActorRef, PoisonPill, Props, Terminated}
import com.hortonworks.orendainx.trucking.simulator.flows.FlowManager
import com.hortonworks.orendainx.trucking.simulator.generators.DataGenerator
import com.typesafe.config.Config

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.Random

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object AutomaticCoordinator {
  case class TickGenerator(generator: ActorRef)

  def props(drivers: Seq[ActorRef], flowManager: ActorRef)(implicit config: Config) =
    Props(new AutomaticCoordinator(drivers, flowManager))
}

class AutomaticCoordinator(generators: Seq[ActorRef], flowManager: ActorRef)(implicit config: Config) extends DriverCoordinator with ActorLogging {

  // For receive messages and an execution context
  import AutomaticCoordinator._
  import DriverCoordinator._
  import context.dispatcher

  // Extract some configs
  val eventCount = config.getInt("options.event-count")
  val eventDelay = config.getInt("simulator.event-delay")
  val eventDelayJitter = config.getInt("simulator.event-delay-jitter")

  // Initialize a drive counter for each data generator
  val generateCounters = mutable.Map(generators.map((_, 0)): _*)

  // Insert each new driver into the simulation (at a random scheduled point) and begin "ticking"
  generators.foreach { generator =>
    context.system.scheduler.scheduleOnce(Random.nextInt(eventDelay + eventDelayJitter).milliseconds, self, TickGenerator(generator))
  }

  // TODO: instead of killing flowmanager, tell parent/manager that it is done/stopped?
  def receive = {
    case AcknowledgeTick(generator) =>
      self ! TickGenerator(generator) // Each ack triggers another tick

    case TickGenerator(generator) =>
      generateCounters.update(generator, generateCounters(generator)+1)

      if (generateCounters(generator) <= eventCount) {
        context.system.scheduler.scheduleOnce((eventDelay + Random.nextInt(eventDelayJitter)).milliseconds, generator, DataGenerator.GenerateData)
      } else {
        // Kill the individual generator as we are done with it.
        generator ! PoisonPill

        // If all other drivers have met their driveCount, kill the transmitter
        if (!generateCounters.values.exists(_ <= eventCount)) {
          flowManager ! FlowManager.Shutdown
          context.watch(flowManager)
        }
      }

    case Terminated(`flowManager`) =>
      // When the eventTransmitter is killed, the system is safe to terminate
      context.system.terminate()
  }

}
