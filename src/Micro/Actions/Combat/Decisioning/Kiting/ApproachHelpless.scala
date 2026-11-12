package Micro.Actions.Combat.Decisioning.Kiting

/**
 * Pure approach-the-helpless decision (no game access): close what cannot
 * hit back. Harmless air always - an Overlord will never punish the walk -
 * buildings at full health, when a wounded hull should keep its range
 * instead. Bounded in land tiles so a distant Shuttle never drags the
 * army across the map (Atlantis Soyuz bound). Distances in tiles.
 */
object ApproachHelpless {

  /** Only close helpless targets inside this land radius. */
  val approachTiles: Double = 12.0

  def shouldClose(cantHitUs: Boolean, flying: Boolean, fullHealth: Boolean, groundDistTiles: Double): Boolean =
    cantHitUs && groundDistTiles <= approachTiles && (flying || fullHealth)
}
