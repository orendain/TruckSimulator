package com.hortonworks.orendainx.trucking.simulator.coordinators

import akka.actor.{ActorLogging, ActorRef, Props}
import com.hortonworks.orendainx.trucking.simulator.generators.DataGenerator
import com.typesafe.config.Config

import scala.collection.mutable

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object ManualCoordinator {
  case object Tick

  def props(generators: Seq[ActorRef])(implicit config: Config) =
    Props(new ManualCoordinator(generators))
}

class ManualCoordinator(generators: Seq[ActorRef])(implicit config: Config) extends DriverCoordinator with ActorLogging {

  // For receive messages
  import DriverCoordinator._
  import ManualCoordinator._

  // Set all generators as ready
  val generatorsReady = mutable.Set(generators: _*)

  def receive = {
    case AcknowledgeTick(drivingAgent) =>
      generatorsReady += drivingAgent
      log.debug(s"Generator acknowledged tick - total: ${generatorsReady.size}")

    case Tick =>
      generatorsReady.foreach(_ ! DataGenerator.GenerateData)
      generatorsReady.clear()
  }

}
