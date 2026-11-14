package Micro.Actions.Protoss.ReaverShuttle

/**
 * Braced Reaver (no game access): a healthy Reaver with a friendly ground
 * warrior beside it is a firing platform, not a fleeing unit.
 *
 * The sim retreats every Reaver it can - the Fallback technique fires for
 * the whole class - so a Reaver logged "Fallback/Retreat" while healthy
 * and escorted, walking away from packs it outranges instead of spending
 * its scarab. The caller feeds health, the escort read, the in-reach
 * target read and the cooldown, all measured from game state.
 */
object ReaverBrave {

  /** Stand and fight at or above this health (Reaver: 100 HP + 50 shields). */
  val braveHealth: Int = 120

  /** A friendly ground combat unit inside this many tiles escorts us. */
  val escortTiles: Double = 5.0

  /** Shoot at what already sits inside scarab reach, in tiles. */
  val reachTiles: Double = 8.0

  def applies(
    isReaver      : Boolean,
    health        : Int,
    escorted      : Boolean,
    targetInReach : Boolean,
    cooldownLeft  : Int): Boolean =
    isReaver &&
      health >= braveHealth &&
      escorted &&
      targetInReach &&
      cooldownLeft <= 0
}
