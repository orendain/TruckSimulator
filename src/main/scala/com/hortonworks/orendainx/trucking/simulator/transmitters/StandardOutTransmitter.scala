package com.hortonworks.orendainx.trucking.simulator.transmitters

import akka.actor.Props
import com.hortonworks.orendainx.trucking.simulator.transmitters.EventTransmitter.TransmitEvent

/**
  * StandardOutTransmitter records events to standard output.
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object StandardOutTransmitter {
  def props() = Props(new StandardOutTransmitter)
}

class StandardOutTransmitter extends EventTransmitter {

  def receive = {
    case TransmitEvent(event) => println(event.toCSV)
  }

}
