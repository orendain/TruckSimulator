package com.hortonworks.orendainx.trucking.simulator.coordinators

import akka.actor.{ActorLogging, ActorRef, Props}
import com.hortonworks.orendainx.trucking.simulator.actors.DrivingAgent
import com.hortonworks.orendainx.trucking.simulator.models.Driver
import com.typesafe.config.Config

import scala.collection.mutable

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object ManualCoordinator {
  case object Tick

  def props(drivers: Seq[Driver], depot: ActorRef, eventTransmitter: ActorRef)(implicit config: Config) =
    Props(new ManualCoordinator(drivers, depot, eventTransmitter))
}

class ManualCoordinator(drivers: Seq[Driver], depot: ActorRef, eventTransmitter: ActorRef)(implicit config: Config) extends DriverCoordinator with ActorLogging {

  // For receive messages and an execution context
  import DriverCoordinator._
  import ManualCoordinator._

  // Create new drivers and set all drivers as ready
  val drivingAgents = drivers.map { driver => context.actorOf(DrivingAgent.props(driver, depot, eventTransmitter)) }
  val drivingAgentsReady = mutable.Set(drivingAgents: _*)

  def receive = {
    case AcknowledgeTick(drivingAgent) =>
      drivingAgentsReady += drivingAgent
      log.debug(s"Someone acknowledged tick - total: ${drivingAgentsReady.size}")

    case Tick =>
      drivingAgentsReady.foreach { drivingAgent =>
        drivingAgent ! DrivingAgent.Drive
      }
      drivingAgentsReady.clear()
  }

}
