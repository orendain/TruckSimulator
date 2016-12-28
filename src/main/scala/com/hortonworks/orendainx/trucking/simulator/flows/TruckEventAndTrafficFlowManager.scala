package com.hortonworks.orendainx.trucking.simulator.flows

import akka.actor.{ActorRef, PoisonPill, Props, Terminated}
import com.hortonworks.orendainx.trucking.shared.models.{TrafficData, TruckEvent}
import com.hortonworks.orendainx.trucking.simulator.flows.FlowManager.Shutdown
import com.hortonworks.orendainx.trucking.simulator.transmitters.DataTransmitter.Transmit

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object TruckEventAndTrafficFlowManager {
  def props(truckTransmitter: ActorRef, trafficTransmitter: ActorRef) =
    Props(new TruckEventAndTrafficFlowManager(truckTransmitter, trafficTransmitter))
}

class TruckEventAndTrafficFlowManager(truckTransmitter: ActorRef, trafficTransmitter: ActorRef) extends FlowManager {

  var transmittersTerminated = 0

  def receive = {
    case Transmit(data: TruckEvent) => truckTransmitter ! Transmit(data)
    case Transmit(data: TrafficData) => trafficTransmitter ! Transmit(data)

    case Shutdown =>
      truckTransmitter ! PoisonPill
      trafficTransmitter ! PoisonPill
      context.watch(truckTransmitter)
      context.watch(trafficTransmitter)

    case Terminated(_) =>
      transmittersTerminated += 1
      if (transmittersTerminated == 2) context.system.terminate()
  }
}
