package Micro.Targeting

/**
 * Demise-gate law for the target ladder (Reaver_v_Sunkens): the ladder
 * prefers targets whose demise is far off so volleys are never wasted
 * on corpses-in-waiting. That overkill avoidance is correct for snipes
 * (limited shots) and for units (shared focus covers them), but static
 * defense holds its stratum while the line burns it down: damaging one
 * Sunken drops it to the reprise stratum, the pick jumps to the fresh
 * twin, and staggered picks trade #1/#13 every few frames forever
 * instead of finishing either. Statics therefore always qualify; the
 * injury bonus inside the ranking picks which one dies first.
 *
 * Pure flags for the suite; the ladder computes the doom distance.
 */
object DemiseGate {

  def qualifies(isStaticDefense: Boolean, doomFar: Boolean): Boolean =
    isStaticDefense || doomFar
}
