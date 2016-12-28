package com.hortonworks.orendainx.trucking.simulator

import akka.actor.ActorSystem
import com.hortonworks.orendainx.trucking.simulator.coordinators.AutomaticCoordinator
import com.hortonworks.orendainx.trucking.simulator.models.{Driver, DrivingPattern}
import com.hortonworks.orendainx.trucking.simulator.transmitters.{FileTransmitter, StandardOutTransmitter}
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
  * Entry point for the simulator.
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
/*
object SimulatorMain {

  def main2(args: Array[String]): Unit = {

    val system = ActorSystem("SimulatorMain")
    implicit val config = ConfigFactory.load()

    // Determine the correct transmitter to initialize
    val transmitterClass = config.getString("options.transmitter")
    val transmitterProps = transmitterClass match {
      case "FileTransmitter" =>
        val filepath = config.getString("options.filetransmitter.filepath")
        FileTransmitter.props(filepath)
      case "StandardOutTransmitter" =>
        StandardOutTransmitter.props()
    }

    // Materialize depot and transmitter actors
    val depot = system.actorOf(NoSharingDepot.props())
    val eventTransmitter = system.actorOf(transmitterProps)

    // Generate driving patterns
    val patterns = config.getConfigList("simulator.driving-patterns").map { conf =>
      val name = conf.getString("name")
      (name, DrivingPattern(name, conf.getInt("min-speed"), conf.getInt("max-speed"), conf.getInt("risk-frequency")))
    }.toMap

    // Generate drivers
    val driverCount = config.getInt("options.driver-count")
    val drivers = {
      // This assumes that special-drivers have sequential ids starting at 1

      // First, initialize all special drivers
      val specialDrivers = config.getConfigList("simulator.special-drivers").map { conf =>
        Driver(conf.getInt("id"), conf.getString("name"), patterns(conf.getString("pattern")))
      }

      // If we need more drivers, generate "normal" drivers. Or if we need to remove some special drivers, do so.
      if (specialDrivers.length < driverCount) {
        val newDrivers = ((specialDrivers.length+1) to driverCount).map { newId =>
          val randomDriverName = Random.alphanumeric.take(config.getInt("simulator.driver-name-length")).mkString
          Driver(newId, randomDriverName, patterns("normal"))
        }
        specialDrivers ++ newDrivers
      } else
        specialDrivers.take(driverCount)
    }

    // Create a DriverCoordinator, beginning the simulation
    system.actorOf(AutomaticCoordinator.props(drivers, depot, eventTransmitter))

    // Ensure that the actor system is properly terminated when the simulator is shutdown.
    scala.sys.addShutdownHook {
      system.terminate()
      Await.result(system.whenTerminated, 15.seconds)
    }
  }
}
*/