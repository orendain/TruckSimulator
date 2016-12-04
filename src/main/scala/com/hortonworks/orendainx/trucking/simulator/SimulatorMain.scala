package com.hortonworks.orendainx.trucking.simulator

import akka.actor.{ActorSystem, Props}
import com.hortonworks.orendainx.trucking.simulator.actors.{DispatcherActor, DriverActor}
import com.hortonworks.orendainx.trucking.simulator.collectors.{EventCollector, FileCollector}
import com.hortonworks.orendainx.trucking.simulator.models.{Driver, DrivingPattern}
import com.hortonworks.orendainx.trucking.simulator.services.RouteParser
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConversions._
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

    // TODO: resolve class based on string
    val collectorClass = config.getString("simulation.collector")
    val collectorFilepath = config.getString("simulation.filecollector.filepath")

    implicit val collector = system.actorOf(FileCollector.props(collectorFilepath))
    implicit val dispatcher = system.actorOf(DispatcherActor.props())


    val eventCount = config.getInt("simulation.event-count")
    val eventDelay = config.getInt("simulation.event-delay")
    val eventDelayJitter = config.getInt("simulation.event-delay-jitter")
    val driverCount = config.getInt("simulation.driver-count")


    val patterns = config.getConfigList("driving.driving-patterns").map { conf =>
      val name = conf.getString("name")
      (name, DrivingPattern(name, conf.getInt("min-speed"), conf.getInt("max-speed"), conf.getInt("risk-frequency")))
    }.toMap

    // TODO: this assumes that special-drivers have sequential ids starting at 1
    val drivers = {
      // Initialize all the special drivers
      val specialDrivers = config.getConfigList("special-drivers").map { conf =>
        Driver(conf.getInt("id"), conf.getString("name"), patterns(conf.getString("pattern")))
      }.toList

      // If we need more drivers, generate "normal" drivers. Or if we need to remove some special drivers, do so.
      if (driverCount - specialDrivers.length > 0) {
        val newDrivers = (specialDrivers.length to driverCount).map { newId =>
          Driver(newId, "NormalDriver", patterns("normal"))
        }
        specialDrivers ++ newDrivers
      } else
        specialDrivers.take(driverCount)
    }

    // Insert each new driver into the simulation, and tell them to "Drive" after every short delay
    drivers.foreach { driver =>
      val actr = system.actorOf(DriverActor.props(driver))
      // TODO: jitter should apply on each tick
      //val delay = (eventDelay - eventDelayJitter) + Random.nextInt(eventDelayJitter * 2 + 1)
      system.scheduler.schedule(0 milliseconds, eventDelay milliseconds, actr, DriverActor.Drive)
    }

  }
}
