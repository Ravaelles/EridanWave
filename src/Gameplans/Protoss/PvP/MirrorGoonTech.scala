package Gameplans.Protoss.PvP

/**
 * Mirror-goon tech (no game access): a Protoss mirror played with Gates
 * and Dragoons stays on Gates and Dragoons - a Robotics Support Bay
 * plus Reaver plus Shuttle is a gas detour the mirror never repays,
 * and the line that orders it loses the goon count that decides the
 * mirror. Committed reaver play (the PvPReaver strategy) and all-ins
 * keep their own say; this only closes the reactive tech reaction to
 * plain goon builds.
 */
object MirrorGoonTech {

  /**
   * Skip reavers (pure numbers for the suite): three- or four-gate
   * goons on the other side mean gas stays on gates, goons and range.
   */
  def skipReavers(fourGateGoon: Boolean, threeGateGoon: Boolean): Boolean =
    fourGateGoon || threeGateGoon
}
