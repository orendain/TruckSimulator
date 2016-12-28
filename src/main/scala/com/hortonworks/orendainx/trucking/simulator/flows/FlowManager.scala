package com.hortonworks.orendainx.trucking.simulator.flows

import akka.actor.Actor

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object FlowManager {
  case object Shutdown
}

trait FlowManager extends Actor
