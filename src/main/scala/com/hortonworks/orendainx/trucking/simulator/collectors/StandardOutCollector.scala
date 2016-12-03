package com.hortonworks.orendainx.trucking.simulator.collectors

import com.hortonworks.orendainx.trucking.simulator.collectors.EventCollector.CollectEvent

/**
  * StandardOutCollector records events to standard output.
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
class StandardOutCollector(filePath: String) extends EventCollector {

  def receive: Unit = {
    case CollectEvent(event) =>
      println(event.toText)
    case _ =>
  }
}
