package com.hortonworks.orendainx.trucking.simulator

import akka.actor.{ActorRef, ActorSystem}
import com.hortonworks.orendainx.trucking.simulator.actors.TruckAndRouteDepot
import com.hortonworks.orendainx.trucking.simulator.coordinators.{AutomaticCoordinator, ManualCoordinator}
import com.hortonworks.orendainx.trucking.simulator.models.{Driver, DrivingPattern}
import com.hortonworks.orendainx.trucking.simulator.transmitters.AccumulateTransmitter
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object NiFiSimulator {
  def apply() = new NiFiSimulator()
}

class NiFiSimulator {

  private val system = ActorSystem("NiFiSimulator")
  private implicit val config = ConfigFactory.load()

  // Create the depot and generate the drivers in the simulation
  private val depot = system.actorOf(TruckAndRouteDepot.props())
  private val drivers = generateDrivers()

  // Create the transmitter and coordinator, set to public
  val transmitter: ActorRef = system.actorOf(AccumulateTransmitter.props())
  val coordinator: ActorRef = system.actorOf(ManualCoordinator.props(drivers, depot, transmitter))

  // Ensure that the actor system is properly terminated when the simulator is shutdown.
  scala.sys.addShutdownHook {
    system.terminate()
    Await.result(system.whenTerminated, 15.seconds)
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
