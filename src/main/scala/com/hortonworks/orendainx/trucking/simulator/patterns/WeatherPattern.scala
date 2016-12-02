package com.hortonworks.orendainx.trucking.simulator.patterns

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
class WeatherPattern(configFilename: String) extends Pattern(configFilename) {

  // TODO: Amortize by unlazy'ing?
  lazy val fogSomething = config.getInt("fog.something")
}
