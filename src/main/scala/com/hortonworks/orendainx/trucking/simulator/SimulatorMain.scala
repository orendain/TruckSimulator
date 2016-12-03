package com.hortonworks.orendainx.trucking.simulator

import akka.actor.{ActorSystem, Props}
import com.hortonworks.orendainx.trucking.simulator.collectors.FileCollector

import scala.concurrent.duration._

// TODO: Implement different entry points either as alternate main objects, or as methods/classes whose name/reference are passed in as args to main

/**
  * Entry point for the simulator.
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object SimulatorMain {

  def main(args: Array[String]): Unit = {

    /*
    com.hortonworks.orendainx.trucking.simulator.emitters.Driver
     */
    val emitter = ???

    /*
    com.hortonworks.orendainx.trucking.simulator.collectors.FileCollector
    com.hortonworks.orendainx.trucking.simulator.collectors.StandardOutCollector
     */
    val collector = ???

    val eventCount: Int = ???
    val eventDelay: Int = ???

    val emitterCount: Int = ???

    val emitterArgs = ???
    val collectorArgs = ???

    val system = ActorSystem("SimulatorMain")

    val props = system.actorOf(Props[FileCollector])

    system.scheduler.schedule(0 milliseconds, eventDelay milliseconds, props, "")

  }
}


/*
options to simulator call

DriverClass
EmitterClass
CollectorClass

eventCountPerEmitter
eventDelay
numberOfEmitters

routeDirectory

OutputLocation (and other args to collectorclass)
*/
