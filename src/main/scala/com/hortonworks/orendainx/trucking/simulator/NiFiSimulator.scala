package com.hortonworks.orendainx.trucking.simulator

import akka.actor.{ActorRef, ActorSystem, Inbox}
import com.hortonworks.orendainx.trucking.shared.models.TruckingData
import com.hortonworks.orendainx.trucking.simulator.coordinators.ManualCoordinator
import com.hortonworks.orendainx.trucking.simulator.depots.NoSharingDepot
import com.hortonworks.orendainx.trucking.simulator.flows.{SharedFlowManager, TruckEventAndTrafficFlowManager}
import com.hortonworks.orendainx.trucking.simulator.generators.TruckAndTrafficGenerator
import com.hortonworks.orendainx.trucking.simulator.models.{Driver, DrivingPattern}
import com.hortonworks.orendainx.trucking.simulator.transmitters.{AccumulateTransmitter, ActorTransmitter}
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
  * A simulator ideal for use by a NiFi processor.
  *
  * This simulator:
  * - Exposes a [[ManualCoordinator]] that can be ticked manually for controlled scheduling.
  * - Exposes an [[akka.actor.Inbox]] that receives all generated [[com.hortonworks.orendainx.trucking.shared.models.TruckingData]].
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object NiFiSimulator {
  def apply() = new NiFiSimulator()
}

class NiFiSimulator {

  private implicit val config = ConfigFactory.load()
  val system = ActorSystem("NiFiSimulator")

  // Create the depot and generate the drivers in the simulation
  private val depot = system.actorOf(NoSharingDepot.props())
  private val drivers = generateDrivers()

  //val truckTransmitter: ActorRef = system.actorOf(AccumulateTransmitter.props())
  //val trafficTransmitter: ActorRef = system.actorOf(AccumulateTransmitter.props())
  //val flowManager = system.actorOf(TruckEventAndTrafficFlowManager.props(truckTransmitter, trafficTransmitter))

  private val inbox = Inbox.create(system)

  private val transmitter = system.actorOf(AccumulateTransmitter.props())
  private val flowManager = system.actorOf(SharedFlowManager.props(transmitter))
  private val dataGenerators = drivers.map { driver => system.actorOf(TruckAndTrafficGenerator.props(driver, depot, flowManager)) }

  // Create the transmitter and coordinator, set to public
  val coordinator: ActorRef = system.actorOf(ManualCoordinator.props(dataGenerators))

  // Ensure that the actor system is properly terminated when the simulator is shutdown.
  scala.sys.addShutdownHook {
    system.terminate()
    Await.result(system.whenTerminated, 10.seconds)
  }

  def fetch(): List[TruckingData] = {
    inbox.send(transmitter, AccumulateTransmitter.Fetch)
    inbox.receive(1.second).asInstanceOf[List[TruckingData]]
  }

  def stop(): Unit = {
    system.terminate()
  }

  /**
    * Generate a list of drivers.
    * Note: This assumes that special-drivers have sequential ids starting at 1
    */
  private def generateDrivers() = {
    // Generate driving patterns
    val patterns = config.getConfigList("simulator.driving-patterns").map { conf =>
      val name = conf.getString("name")
      (name, DrivingPattern(name, conf.getInt("min-speed"), conf.getInt("max-speed"), conf.getInt("risk-frequency")))
    }.toMap

    // First, initialize all special drivers
    val specialDrivers = config.getConfigList("simulator.special-drivers").map { conf =>
      Driver(conf.getInt("id"), conf.getString("name"), patterns(conf.getString("pattern")))
    }

    // If we need more drivers, generate "normal" drivers. Or if we need to remove some special drivers, do so.
    val driverCount = config.getInt("options.driver-count")
    if (specialDrivers.length < driverCount) {
      val newDrivers = ((specialDrivers.length+1) to driverCount).map { newId =>
        val randomDriverName = Random.alphanumeric.take(config.getInt("simulator.driver-name-length")).mkString
        Driver(newId, randomDriverName, patterns("normal"))
      }
      specialDrivers ++ newDrivers
    } else
      specialDrivers.take(driverCount)
  }
}
