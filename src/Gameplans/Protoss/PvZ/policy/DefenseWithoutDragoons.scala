package Gameplans.Protoss.PvZ.policy

import Information.Battles.ArmyStrength

/**
 * Pure broke-without-Dragoons gate (no game access): vs Zerg with no
 * Dragoons, at most a handful of Zealots and under 165 strength the army
 * cannot trade Hydralisks, Lurkers or Mutalisks - it falls back to defense
 * instead of feeding. Dragoons present, a real Zealot force, non-Zerg enemy
 * or clear strength keep the normal posture.
 */
object DefenseWithoutDragoons {

  /** At or below this global strength the army waits for Dragoons. */
  val defendBelowStrength: Double = 165.0

  /** Past this many Zealots the army may fight on without Dragoons. */
  val defendMaxZealots: Int = 6

  def shouldDefend(enemyIsZerg: Boolean, dragoons: Int, zealots: Int, ours: Double, foe: Double): Boolean =
    enemyIsZerg && dragoons == 0 && zealots <= defendMaxZealots && foe > 0 && ArmyStrength.score(ours, foe) <= defendBelowStrength
}
