package Macro.Actions

/**
 * Base-cancel vs Dark Templar (no game access): one home for the "should
 * the morphing base die so detection can live" question. A Nexus caught
 * morphing with a shown enemy DT and no Forge money is a 400-mineral
 * donation - the DT walks in for free and the base never pays back.
 * Canceling refunds three quarters of the cost, which funds the Forge
 * itself and leaves the rest toward the Pylon and Cannon. Only the
 * three stated facts decide; everything else (which base, whether a
 * Forge is already morphing, cannon placement) stays with its owner.
 */
object ShouldCancelBase {

  def shouldCancel(
    enemyDarkTemplarShown: Boolean,
    baseInProduction: Boolean,
    forgeAffordable: Boolean): Boolean =
    enemyDarkTemplarShown && baseInProduction && ! forgeAffordable
}
