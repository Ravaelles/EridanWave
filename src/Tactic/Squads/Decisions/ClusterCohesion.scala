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
   * Farthest any warrior may operate from its cluster, in tiles, victim
   * or not: past this line a lone chase is a donation, not pressure -
   * the hull walks home even with a victim assigned, instead of dueling
   * alone a screen away while the pack fights without it.
   */
  val maxOperateTiles: Double = 8.0

  /**
   * Operate release band, in tiles: a forced hull walks all the way back
   * inside this before the duel may take it again, so the chase and the
   * leash cannot trade the unit every frame on the boundary.
   */
  val operateReleaseTiles: Double = 7.0

  /**
   * Forced return (pure flags for the suite): the drift band above needs
   * an empty slot, but a chase victim is exactly what walks blades out
   * alone - so past the operate line the victim stops mattering and the
   * hull comes home anyway. Every other gate of the drift band stays:
   * explicit orders, retreats, withdrawals, marches, raids in contact
   * and latched duels keep their frames.
   */
  def mustRejoin(
    isWarrior: Boolean,
    retreating: Boolean,
    coolingUnderFire: Boolean,
    inBattle: Boolean,
    macroGame: Boolean,
    hasLocalContact: Boolean,
    verdictDuel: Boolean,
    distTiles: Double): Boolean =
    isWarrior && ! retreating && ! coolingUnderFire &&
      inBattle && macroGame && ! hasLocalContact && ! verdictDuel && distTiles > maxOperateTiles

  /**
   * Rejoin when all hold (pure flags for the suite): a warrior with no
   * victim, not retreating, not cooling under fire, inside a macro-game
   * battle, without local contact, past the drift band, and with no
   * latched duel - the march already holds a live duel under its feet,
   * so yanking the hull to the centroid is the flap, not cohesion. The
   * caller owns the explicit-order exemptions (scouts, commits, suicides,
   * yolo).
   */
  def shouldRejoin(
    isWarrior: Boolean,
    hasTarget: Boolean,
    retreating: Boolean,
    coolingUnderFire: Boolean,
    inBattle: Boolean,
    macroGame: Boolean,
    hasLocalContact: Boolean,
    verdictDuel: Boolean,
    distTiles: Double): Boolean =
    isWarrior && ! hasTarget && ! retreating && ! coolingUnderFire &&
      inBattle && macroGame && ! hasLocalContact && ! verdictDuel && distTiles > maxDriftTiles
}
