package com.hortonworks.orendainx.trucking.simulator.generators

import akka.actor.Actor

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object DataGenerator {
  case object GenerateData

  // TODO: Ungenericize arg?
  case class NewResource(resource: Any)
}

trait DataGenerator extends Actor
