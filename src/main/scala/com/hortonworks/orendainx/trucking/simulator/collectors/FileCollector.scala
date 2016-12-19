package com.hortonworks.orendainx.trucking.simulator.collectors

import akka.actor.{ActorLogging, Props}
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

class FileCollector(filepath: String) extends EventCollector with ActorLogging {

  private val writer = File(filepath).createIfNotExists(createParents = true).newBufferedWriter

  def receive = {
    case CollectEvent(event) =>
      writer.write(event.toText)
      writer.newLine()
  }

  override def postStop(): Unit = {
    writer.close()
    log.info("FileCollector closed output file stream.")
  }
}
