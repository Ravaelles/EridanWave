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
}
