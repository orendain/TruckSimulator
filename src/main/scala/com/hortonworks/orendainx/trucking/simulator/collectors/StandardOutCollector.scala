package com.hortonworks.orendainx.trucking.simulator.collectors

import akka.actor.Props
import com.hortonworks.orendainx.trucking.simulator.collectors.EventCollector.CollectEvent

/**
  * StandardOutCollector records events to standard output.
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object StandardOutCollector {
  def props = Props(new StandardOutCollector)
}

class StandardOutCollector extends EventCollector {

  def receive = {
    case CollectEvent(event) => println(event.toText)
    case _ =>
  }

}
