package com.hortonworks.orendainx.trucking.simulator

import akka.actor.ActorSystem
import com.hortonworks.orendainx.trucking.simulator.actors.{DriverCoordinator, TruckAndRouteDepot}
import com.hortonworks.orendainx.trucking.simulator.collectors.{FileCollector, StandardOutCollector}
import com.hortonworks.orendainx.trucking.simulator.models.{Driver, DrivingPattern}
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
object SimulatorMain {

  def main(args: Array[String]): Unit = {

    val system = ActorSystem("SimulatorMain")
    implicit val config = ConfigFactory.load()

    // Determine the correct collector to initialize
    val collectorClass = config.getString("options.collector")
    val collectorProps = collectorClass match {
      case "FileCollector" =>
        val filepath = config.getString("options.filecollector.filepath")
        FileCollector.props(filepath)
      case "StandardOutCollector" =>
        StandardOutCollector.props()
    }

    // Materialize dispatcher and collector actors
    val dispatcher = system.actorOf(TruckAndRouteDepot.props())
    val eventCollector = system.actorOf(collectorProps)

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
        val newDrivers = (specialDrivers.length to driverCount).map { newId =>
          val randomDriverName = Random.nextString(config.getInt("simulator.driver-name-length"))
          Driver(newId, randomDriverName, patterns("normal"))
        }
        specialDrivers ++ newDrivers
      } else
        specialDrivers.take(driverCount)
    }

    // Create a DriverCoordinator, beginning the simulation
    system.actorOf(DriverCoordinator.props(drivers, dispatcher, eventCollector))

    // Ensure that the actor system is properly terminated when the simulator is shutdown.
    scala.sys.addShutdownHook {
      system.terminate()
      Await.result(system.whenTerminated, 15.seconds)
    }
  }
}
