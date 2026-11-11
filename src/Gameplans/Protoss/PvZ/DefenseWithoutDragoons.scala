package Gameplans.Protoss.PvZ

import Information.Battles.ArmyStrength

/**
 * Pure broke-without-Dragoons gate (no game access): vs Zerg with no
 * Dragoons under 130 strength the army cannot trade Hydralisks, Lurkers
 * or Mutalisks - it falls back to defense instead of feeding. Dragoons
 * present, non-Zerg enemy or even strength keep the normal posture.
 */
object DefenseWithoutDragoons {

  def shouldDefend(enemyIsZerg: Boolean, dragoons: Int, ours: Double, foe: Double): Boolean =
    enemyIsZerg && dragoons == 0 && ArmyStrength.retreating(ours, foe)
}
