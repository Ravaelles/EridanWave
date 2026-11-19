package Gameplans.Protoss.PvT

/**
 * Forward gateways (pure numbers, no game access): a banked Protoss
 * spends the stockpile on production instead of sitting on it, and
 * with three bases the new gates rise closer to the enemy than our
 * own natural choke sits - the walk to the fight shrinks instead of
 * the reinforcements commuting the whole map. The caller counts the
 * minerals and the bases; each expo that beats the choke reach owns
 * its gates.
 */
object ForwardGateways {

  /** Spend the bank at or above this stockpile, in minerals. */
  val bankMinerals: Int = 800

  /** Banked production floor: this many gates, at least. */
  val minGateways: Int = 13

  /** Build forward once this many bases stand. */
  val minBases: Int = 3

  def shouldBankGates(minerals: Int): Boolean =
    minerals > bankMinerals

  def shouldBuildForward(bases: Int): Boolean =
    bases >= minBases

  /** Forward means closer to the enemy than our natural choke, in tiles. */
  def isForward(distToEnemyTiles: Double, chokeToEnemyTiles: Double): Boolean =
    distToEnemyTiles < chokeToEnemyTiles
}
