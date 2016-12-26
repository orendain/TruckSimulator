package com.hortonworks.orendainx.trucking.simulator.transmitters

import akka.actor.{ActorLogging, Props}
import com.hortonworks.orendainx.trucking.shared.models.Event
import com.hortonworks.orendainx.trucking.simulator.transmitters.AccumulateTransmitter.Fetch
import com.hortonworks.orendainx.trucking.simulator.transmitters.EventTransmitter.TransmitEvent

import scala.collection.mutable

/**
  * AccumulateTransmitter stores events to be polled by an outside source.
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object AccumulateTransmitter {
  case object Fetch

  def props() = Props(new AccumulateTransmitter)
}

class AccumulateTransmitter extends EventTransmitter with ActorLogging {

  val buffer = mutable.ListBuffer.empty[Event]

  def receive = {
    case TransmitEvent(event) => buffer += event

    case Fetch =>
      sender() ! buffer.toList
      buffer.clear()
  }

  override def postStop(): Unit = {
    log.info(s"AccumulateTransmitter stopped with ${buffer.length} items unfetched.")
  }
}
