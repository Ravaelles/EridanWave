package Micro.Actions.Protoss.Reaver

/**
 * Pure static standoff (no game access): a Reaver outranges every static
 * defense, so sitting inside their reach is pure donation. Inside the
 * foe's reach plus a half-tile edge the hull backs off to its own max
 * range instead of holding - Scarabs fly 8 tiles, Sunkens answer at 7.
 * The caller finds the pressing static; this only combines distances.
 */
object ReaverStaticStandoff {

  /** Back off while this deep inside the foe's reach, in tiles. */
  val edgeTiles: Double = 0.5

  def shouldBackOff(distTiles: Double, ownRangeTiles: Double, foeRangeTiles: Double): Boolean =
    ownRangeTiles > foeRangeTiles && distTiles < foeRangeTiles + edgeTiles

  /** Ground to give to land exactly on our max range, in tiles. */
  def backoffTiles(distTiles: Double, ownRangeTiles: Double): Double =
    Math.max(0, ownRangeTiles - distTiles)
}
