package Micro.Actions.Combat.Decisioning.Kiting

/**
 * Pure approach-the-helpless decision (no game access): close what cannot
 * hit back. Harmless air always - an Overlord will never punish the walk -
 * grounded helpless (buildings, fleeing workers) while the shields are at
 * most scratched, when a holed hull should keep its range instead. Bounded
 * in land tiles so a distant Shuttle never drags the army across the map
 * (Atlantis Soyuz bound). Distances in tiles, wounds in shield points.
 */
object ApproachHelpless {

  /** Only close helpless targets inside this land radius. */
  val approachTiles: Double = 12.0

  /** Close grounded helpless while at most this much shield is gone. */
  val shieldWoundMax: Int = 15

  def shouldClose(cantHitUs: Boolean, flying: Boolean, shieldWound: Int, groundDistTiles: Double): Boolean =
    cantHitUs && groundDistTiles <= approachTiles && (flying || shieldWound <= shieldWoundMax)
}
