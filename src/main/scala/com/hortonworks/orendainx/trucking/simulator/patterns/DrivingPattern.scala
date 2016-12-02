package com.hortonworks.orendainx.trucking.simulator.patterns

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
class DrivingPattern(configFilename: String) extends Pattern(configFilename) {

  // TODO: Amortize by unlazy'ing?
  lazy val minSpeed = config.getInt("speed.min")
  lazy val maxSpeed = config.getInt("speed.max")
}
