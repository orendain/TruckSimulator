package com.hortonworks.orendainx.trucking.simulator.collectors

import akka.actor.Props
import better.files.File
import com.hortonworks.orendainx.trucking.simulator.collectors.EventCollector.CollectEvent

/**
  * FileCollector records events to the filesystem, specifically to the file whose path is passed in as the constructor parameter.
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object FileCollector {
  def props(filepath: String) = Props(new FileCollector(filepath))
}

class FileCollector(filepath: String) extends EventCollector {

  private val writer = File(filepath).newBufferedWriter

  def receive: Unit = {
    case CollectEvent(event) =>
      writer.write(event.toText)
      writer.newLine()
    case _ =>
  }

  override def postStop(): Unit = {
    writer.flush()
    writer.close()
  }
}
