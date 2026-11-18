package Micro.Targeting.FiltersRequired

/**
 * Pure Reaver target law (no game access): Scarabs are precious, so a
 * building is worth one only when it shoots back. Combat buildings are
 * line - the Bunker qualifies through its housed Marines
 * effective damage - while Batteries and idle colonies are the
 * army's job.
 * Pure flags for the suite.
 */
object ReaverTargetRules {

  def allowsBuilding(isBuilding: Boolean, isCombatBuilding: Boolean): Boolean =
    ! isBuilding || isCombatBuilding

  /**
   * Escort-first (Reaver_v_Sunkens): a scarab spent on a static while a
   * hittable pack closes is a wasted scarab - the pack kills the hull
   * before the static falls. While a mobile threat stands in range the
   * buildings yield and the ladder can only pick the escort. With the
   * escort clear the contain law still holds: a warrior-less hull
   * breaks statics instead of chasing miners.
   * Pure flags for the suite; the game seam reads the threat band.
   */
  def yieldsToEscort(isBuilding: Boolean, mobileThreatInRange: Boolean): Boolean =
    ! isBuilding || ! mobileThreatInRange
}
