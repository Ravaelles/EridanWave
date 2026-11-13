package Micro.Targeting

/**
 * Repairer snipe: a healthy unit tasked against a Terran building kills
 * the SCV currently repairing it first - unless the building is nearly
 * dead, in which case it finishes the building instead. Enforced even a
 * step out of normal range: walking the extra tile beats trading against
 * free repairs. Pure numbers and flags; the caller resolves which SCV
 * repairs which building.
 */
object RepairerSnipe {

  /** Our unit needs at least this much total HP to go out of its way. */
  val healthyHp: Int = 80

  /** Buildings this close to death get finished instead of diverted from. */
  val doomedBuildingHp: Int = 30

  /** Repairers count as hittable this far past our normal reach, in tiles. */
  val chaseTiles: Double = 1.5

  def shouldSnipe(actorHp: Int, buildingHp: Int): Boolean =
    actorHp >= healthyHp && buildingHp >= doomedBuildingHp

  def inChaseRange(distTiles: Double, ownRangeTiles: Double): Boolean =
    distTiles <= ownRangeTiles + chaseTiles
}
