package com.hortonworks.orendainx.trucking.simulator.collectors

import better.files.File
import com.hortonworks.orendainx.trucking.simulator.collectors.EventCollector.CollectEvent

/**
  * FileCollector records events to the filesystem, specifically to the file whose path is passed in as the constructor parameter.
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
class FileCollector(filePath: String) extends EventCollector {

  val writer = File(filePath).newBufferedWriter

  def receive: Unit = {
    case CollectEvent(event) =>
      writer.write(event.toText)
      writer.newLine()
    case _ =>
  }
}
