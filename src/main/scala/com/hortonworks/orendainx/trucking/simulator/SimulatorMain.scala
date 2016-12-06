package com.hortonworks.orendainx.trucking.simulator

import akka.actor.{ActorSystem, Props}
import com.hortonworks.orendainx.trucking.simulator.actors.{TruckAndRouteDepot, DriverActor, DriverCoordinator}
import com.hortonworks.orendainx.trucking.simulator.collectors.{EventCollector, FileCollector}
import com.hortonworks.orendainx.trucking.simulator.models.{Driver, DrivingPattern}
import com.hortonworks.orendainx.trucking.simulator.services.RouteParser
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

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
    val collectorClass = config.getString("options.collector") // TODO: resolve class based on string
    val collectorFilepath = config.getString("options.filecollector.filepath")

    // Materialize dispatcher and collector actors
    val dispatcher = system.actorOf(TruckAndRouteDepot.props())
    val eventCollector = system.actorOf(FileCollector.props(collectorFilepath))

    // Generate driving patterns
    val patterns = config.getConfigList("simulator.driving-patterns").map { conf =>
      val name = conf.getString("name")
      (name, DrivingPattern(name, conf.getInt("min-speed"), conf.getInt("max-speed"), conf.getInt("risk-frequency")))
    }.toMap

    // Generate drivers
    val driverCount = config.getInt("options.driver-count")
    val drivers = {
      // TODO: this assumes that special-drivers have sequential ids starting at 1

      // First, initialize all special drivers
      val specialDrivers = config.getConfigList("special-drivers").map { conf =>
        Driver(conf.getInt("id"), conf.getString("name"), patterns(conf.getString("pattern")))
      }

      // If we need more drivers, generate "normal" drivers. Or if we need to remove some special drivers, do so.
      if (specialDrivers.length < driverCount) {
        val newDrivers = (specialDrivers.length to driverCount).map { newId =>
          Driver(newId, "NormalDriverName", patterns("normal")) // TODO: generate driver names
        }
        specialDrivers ++ newDrivers
      } else
        specialDrivers.take(driverCount)
    }

    // Create a DriverCoordinator, beginning the simulation
    system.actorOf(DriverCoordinator.props(drivers, dispatcher, eventCollector))
  }
}
