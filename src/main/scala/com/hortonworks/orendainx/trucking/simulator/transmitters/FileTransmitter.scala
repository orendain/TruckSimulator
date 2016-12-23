package com.hortonworks.orendainx.trucking.simulator.transmitters

import akka.actor.{ActorLogging, Props}
import better.files.File
import com.hortonworks.orendainx.trucking.simulator.transmitters.EventTransmitter.TransmitEvent

/**
  * FileTransmitter records events to the filesystem, specifically to the file whose path is passed in as the constructor parameter.
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
object FileTransmitter {
  def props(filepath: String) = Props(new FileTransmitter(filepath))
}

class FileTransmitter(filepath: String) extends EventTransmitter with ActorLogging {

  private val writer = File(filepath).createIfNotExists(createParents = true).newPrintWriter(true)

  def receive = {
    case TransmitEvent(event) => writer.println(event.toCSV)
  }

  override def postStop(): Unit = {
    writer.close()
    log.info("FileTransmitter closed output file stream.")
  }
}
