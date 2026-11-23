package Tactic.Squads.Decisions

import Mathematics.Points.Pixel

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
  armyUnitsScore   : Double,
  supplyUsed       : Int,
  proxyRush        : Boolean,
  home             : Option[Mathematics.Points.Pixel],
  catchUpAnchor    : Option[Pixel],
  alphaAnchorTiles : Double) {

  /** A toothless Terran fields nothing worth forming up against. */
  def enemyIsToothless: Boolean = enemyIsTerran && ! enemyHasTanks && ! enemyHasVultures
}
