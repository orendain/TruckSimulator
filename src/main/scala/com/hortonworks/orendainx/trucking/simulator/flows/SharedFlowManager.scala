package com.hortonworks.orendainx.trucking.simulator.flows

import akka.actor.{ActorRef, PoisonPill, Props, Terminated}
import com.hortonworks.orendainx.trucking.simulator.flows.FlowManager.Shutdown
import com.hortonworks.orendainx.trucking.simulator.transmitters.DataTransmitter.Transmit

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object SharedFlowManager {
  def props(transmitter: ActorRef) =
    Props(new SharedFlowManager(transmitter))
}
class SharedFlowManager(transmitter: ActorRef) extends FlowManager {
  def receive = {
    case msg: Transmit => transmitter ! msg

    case Shutdown =>
      transmitter ! PoisonPill
      context.watch(transmitter)

    case Terminated(`transmitter`) =>
      context.system.terminate()
  }
}
