package Tactic.Squads.Decisions

/**
 * Retreat permission (no game access): nothing retreats from an empty
 * field, and a proxy rush is answered, never fled from. Retreating with
 * no armed enemy around is how grinds stall for minutes - the verdict
 * flaps between the sim fear and the empty field instead of holding
 * the line, and every flip drags the march home and back. Both inputs
 * are squad-scale on purpose: this gates the cluster verdict and the
 * march home, never individual duels - a dying hull still runs on its
 * own hit points below. Distances in tiles.
 */
object RetreatAllowed {

  /**
   * Units score at or above which the cluster attacks without asking
   * the sim: the forecast already had its say through the fighting
   * fraction, and a standing army does not walk home. Hard counters
   * keep the old vote - lurkers, Dark Templar, carriers and dug-in
   * siege break lines the score cannot see.
   */
  val attackUnitsScore: Double = 180.0

  /** Past this an armed enemy is not near the cluster, in tiles. */
  val armedNearTiles: Double = 12.0

  /** Dying hulls may always run, in total hit points. */
  val dyingHp: Int = 35

  /**
   * Cluster retreat allowed (pure numbers for the suite): weak clusters
   * may retreat from a near enemy, never from an empty field - without
   * this the whole army routes on frame zero and yo-yos whenever the
   * sim blinks (Crona min-5 rout with zero enemies seen). Strong ones
   * need the armed enemy close enough to matter. A proxy rush forbids
   * it outright - marines at our door are answered, not fled from.
   */
  def forCluster(unitsScore: Double, nearestArmedTiles: Double, proxyRush: Boolean, hardCounterKnown: Boolean): Boolean =
    ! proxyRush &&
      (unitsScore < attackUnitsScore || hardCounterKnown) &&
      nearestArmedTiles <= armedNearTiles

  /**
   * Unit retreat allowed (pure numbers for the suite): the cluster
   * verdict stands, except a dying hull always keeps its own way out.
   * The hit-points gate is the only per-unit fact the cluster verdict
   * does not own.
   */
  def forUnit(totalHp: Int, unitsScore: Double, nearestArmedTiles: Double, proxyRush: Boolean, hardCounterKnown: Boolean): Boolean =
    totalHp <= dyingHp || forCluster(unitsScore, nearestArmedTiles, proxyRush, hardCounterKnown)
}
