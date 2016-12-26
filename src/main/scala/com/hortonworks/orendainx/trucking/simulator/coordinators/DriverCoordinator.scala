package com.hortonworks.orendainx.trucking.simulator.coordinators

import akka.actor.{Actor, ActorRef}

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object DriverCoordinator {
  case class AcknowledgeTick(drivingAgent: ActorRef)
}

trait DriverCoordinator extends Actor {

}
