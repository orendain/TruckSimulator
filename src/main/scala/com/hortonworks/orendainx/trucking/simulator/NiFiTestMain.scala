package com.hortonworks.orendainx.trucking.simulator

import akka.actor.Inbox
import com.hortonworks.orendainx.trucking.simulator.coordinators.ManualCoordinator
import com.hortonworks.orendainx.trucking.simulator.transmitters.AccumulateTransmitter
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object NiFiTestMain {

  def main(args: Array[String]): Unit = {
    val obj = new NiFiTestMain()
    obj.init()
  }
}

class NiFiTestMain {
  lazy val config = ConfigFactory.load()
  lazy val simulator = NiFiSimulator()
  lazy val inbox = Inbox.create(simulator.system)

  def init() = {
    inbox.send(simulator.coordinator, ManualCoordinator.Tick)
  }

  def onTrigger() = {

    //simulator.coordinator ! ManualCoordinator.Tick

    // Fetch the results to be retrieved by onTrigger's next invocation
    //simulator.transmitter ! AccumulateTransmitter.Fetch
    inbox.send(simulator.transmitter, AccumulateTransmitter.Fetch)

    // TODO: wait only a super short amount of time ... make into property
    val lst = inbox.receive(500.milliseconds)

    lst match {
      case events: List[_] =>
        events.foreach { event =>
          println(event)
        }
    }

    inbox.send(simulator.coordinator, ManualCoordinator.Tick)

    //simulator.coordinator ! ManualCoordinator.Tick

    // Fetch the results to be retrieved by onTrigger's next invocation
    //simulator.transmitter ! AccumulateTransmitter.Fetch
  }
}
