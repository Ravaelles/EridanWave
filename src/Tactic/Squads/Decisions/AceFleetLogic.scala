package Tactic.Squads.Decisions

/**
 * Pure fleet-size rule for Ace pilots (no game access): enough units to
 * contend enemy air, at least six, unless unopposed splash needs none.
 */
object AceFleetLogic {

  def hasFleet(ours: Int, weSplash: Boolean, enemySplashes: Boolean, enemies: Int): Boolean =
    ours >= Math.max(6, if (weSplash && ! enemySplashes) 0 else enemies)
}
