package Micro.Targeting

/**
 * Remembered-target recall (no game access): how far a duel walks to a
 * remembered fix. Matchups are zone-scoped, so a tracked base across a
 * choke is in neither the live set nor the assigned set, and the line
 * dresses instead of shooting - dragoons stood staring at a base they
 * could not target. The recall horizon keeps the fallback local: past
 * it the march owns movement, never the duel. Same band as
 * FormationHold.holdPursuitTiles (a target past the pursuit horizon
 * re-arms the hunt there); one band, two readers.
 */
object TargetRecall {

  /** Pursuit horizon for remembered targets, in tiles. */
  val recallTiles: Double = 12.0

  /** Walk to the remembered fix inside the horizon, never past it. */
  def shouldRecall(distTiles: Double): Boolean =
    distTiles <= recallTiles
}
