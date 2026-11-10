package Micro.Actions.Protoss.Reaver

/**
 * Pure unload veto (no game access): three-plus ranged enemies able to hit
 * the landing inside their own range plus a safety margin means the drop
 * never goes in. Mobile guns only - statics already vote through unload
 * safety, bravery and contain, and melee answers to the close ring.
 * Atlantis spells it rangedEnemiesThatCanAttackUs(3.1).atLeast(3); here
 * the rule lives in a kernel with a suite instead of a query DSL, because
 * per-frame fluent queries over live buffers duplicate the matchups
 * caches for readability the kernel name already gives.
 */
object ReaverUnloadVeto {

  /** Enemy weapon range bonus, in tiles, pricing the landing. */
  val safetyMarginTiles: Double = 3.1

  /** This many ranged guns in margin veto the drop. */
  val maxRangedAttackers: Int = 2

  def veto(rangedAttackersInMargin: Int): Boolean =
    rangedAttackersInMargin > maxRangedAttackers
}
