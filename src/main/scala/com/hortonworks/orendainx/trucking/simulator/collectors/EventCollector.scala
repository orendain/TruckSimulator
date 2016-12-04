package com.hortonworks.orendainx.trucking.simulator.collectors

import akka.actor.Actor
import com.hortonworks.orendainx.trucking.shared.models.Event

object EventCollector {
  case class CollectEvent(event: Event)
}

/**
  * EventCollectors are the sinks for generated simulator data.
  * They may record events to filesystems, stores over a network, standard out, etc.
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
trait EventCollector extends Actor

/*
 * A valid concern could be that by using actors over some singleton service that is equally as threadsafe, we are employing an antipattern.
 * However, considering not all EventCollectors may leverage a data structure that can be accessed concurrently, using actors
 * to regulate resource access is a much better way (and cleaner) way to approach the problem.
 */
