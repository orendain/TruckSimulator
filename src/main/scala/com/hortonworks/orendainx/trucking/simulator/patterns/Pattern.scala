package com.hortonworks.orendainx.trucking.simulator.patterns

import better.files.File
import com.typesafe.config.ConfigFactory

/**
  * @author Edgar Orendain <edgar@orendainx.com>
  */
abstract class Pattern(configFilename: String) {

  //private val config = ConfigFactory.parseFile(File(configFilepath))
  protected lazy val config = ConfigFactory.load(configFilename)
}
