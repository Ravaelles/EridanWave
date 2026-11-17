package Tactic.Squads.Decisions

/**
 * World facts the gates read, measured once per `sendUnits` instead of once
 * per gate per unit: ten gates over forty units used to mean hundreds of
 * full enemy-list scans a frame on the march path.
 */
final case class MarchFacts(
  enemyIsTerran    : Boolean,
  enemyIsProtoss   : Boolean,
  enemyHasTanks    : Boolean,
  enemyHasVultures : Boolean,
  enemyDragoonSeen : Boolean,
  bankedMinerals   : Int,
  armyStrength     : Double,
  supplyUsed       : Int,
  home             : Option[Mathematics.Points.Pixel]) {

  /** A toothless Terran fields nothing worth forming up against. */
  def enemyIsToothless: Boolean = enemyIsTerran && ! enemyHasTanks && ! enemyHasVultures
}
