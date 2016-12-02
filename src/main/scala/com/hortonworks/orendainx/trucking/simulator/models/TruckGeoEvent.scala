package com.hortonworks.orendainx.trucking.simulator.models

import java.sql.Timestamp


case class TruckGeoEvent(eventTime: Timestamp, truckId: Int, driverId: Int, driverName: String,
                         routeId: Int, routeName: String, status: String, latitude: Double, longitude: Double,
                         correlationId: Long) extends Event {

  override def toText = {
    s"${eventTime.getTime}|$truckId|$driverId|$driverName|$routeId|$routeName|$status|$latitude|$longitude|$correlationId"
  }
}
