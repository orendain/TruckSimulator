package com.hortonworks.orendainx.trucking.simulator.models.events

import java.sql.Timestamp

/**
  * The model for a geo event.  Until the data simulator is integrated with a scheme registry, this is the file
  * that needs to be edited whenever the data the model should output needs changing.
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
case class TruckGeoEvent(eventTime: Timestamp, truckId: Int, driverId: Int, driverName: String,
                         routeId: Int, routeName: String, status: String, latitude: Double, longitude: Double,
                         correlationId: Long) extends Event {

  lazy val toText: String =
    s"${eventTime.getTime}|$truckId|$driverId|$driverName|$routeId|$routeName|$status|$latitude|$longitude|$correlationId"
}
