package ProxyBwapi.UnitClasses

import bwapi.UnitType

object UnitClasses {
  // Static instance graph: UnitTypes are enum data, UnitClass is immutable
  // with value equality, so tests can build classes with no game behind
  // With.proxy. ProxyBWAPI keeps its own copies; all compare equal.
  private lazy val staticAll: Vector[UnitClass] = UnitTypes.all.map(UnitClass)
  private lazy val staticById: Vector[UnitClass] = {
    val maxId = staticAll.map(_.id).max
    (0 to maxId).map(i => staticAll.find(_.id == i)
      .getOrElse(staticAll.find(_.bwapiType == UnitType.None).get)).toVector
  }
  def all: Vector[UnitClass] = staticAll
  def get(unitType: UnitType): UnitClass = staticById(unitType.id)
  lazy val None: UnitClass = all.find(_.bwapiType == UnitType.None).get
  lazy val Unknown: UnitClass = all.find(_.bwapiType == UnitType.Unknown).get
}
