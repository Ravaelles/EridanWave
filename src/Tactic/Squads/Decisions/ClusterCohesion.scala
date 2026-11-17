package Tactic.Squads.Decisions

/**
 * Cluster cohesion (no game access): a combat unit moving without a
 * victim, neither retreating nor cooling under fire, never drifts past
 * fourteen tiles from its cluster's centroid. Inside a battle that unit
 * is lost - walking at ghosts, stale slots or old orders - while the
 * cluster fights without it; outside battle (marches, raids en route,
 * baseless maps) every leg keeps its own rules and this stays out.
 * Raiders mid-fight keep their contact exemption: enemies near the hull
 * mean a local fight, never drift.
 */
object ClusterCohesion {

  /** Farthest a drifting warrior may operate from its cluster, in tiles. */
  val maxDriftTiles: Double = 14.0

  /**
   * Rejoin when all hold (pure flags for the suite): a warrior with no
   * victim, not retreating, not cooling under fire, inside a macro-game
   * battle, without local contact, past the drift band. The caller owns
   * the explicit-order exemptions (scouts, commits, suicides, yolo).
   */
  def shouldRejoin(
    isWarrior: Boolean,
    hasTarget: Boolean,
    retreating: Boolean,
    coolingUnderFire: Boolean,
    inBattle: Boolean,
    macroGame: Boolean,
    hasLocalContact: Boolean,
    distTiles: Double): Boolean =
    isWarrior && ! hasTarget && ! retreating && ! coolingUnderFire &&
      inBattle && macroGame && ! hasLocalContact && distTiles > maxDriftTiles
}
