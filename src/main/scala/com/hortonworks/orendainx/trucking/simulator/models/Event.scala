package com.hortonworks.orendainx.trucking.simulator.models

/**
  * The supertype for all models that expect to pass through the simulator.
  * Extending this type ensures that a data model meets the appropriate requirements and has
  * the necessary apply/unapply/serialize/deserialize/etc. methods that EventCollectors and other
  * possible components need to call to transform and act on the data.
  *
  * Acts as a lighter alternative to a schema registry.
  *
  * @author Edgar Orendain <edgar@orendainx.com>
  */
trait Event extends Serializable {

  /**
    * @return A text representation of the data that is appropriate for storage in external sources.
    */
  def toText: String
}

/*
 Event states:
 Blueprint Obj (case class)
 Serialized to text (e.g. for file)
 Deserialized (back into case class)
 Transformation (case class transformed into another case class)
 */
