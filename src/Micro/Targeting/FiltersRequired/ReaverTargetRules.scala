package Micro.Targeting.FiltersRequired

/**
 * Pure Reaver target law (no game access): Scarabs are precious, so a
 * building is worth one only when it shoots back. Static defense is the
 * line - it covers the Bunker (housed Marines, no weapon of its own)
 * alongside real guns; every other building is the army's job, which
 * also keeps Reavers off the Creep Colonies the army is now told to
 * prioritize. Pure flags for the suite.
 */
object ReaverTargetRules {

  def allowsBuilding(isBuilding: Boolean, isStaticDefense: Boolean): Boolean =
    ! isBuilding || isStaticDefense
}
