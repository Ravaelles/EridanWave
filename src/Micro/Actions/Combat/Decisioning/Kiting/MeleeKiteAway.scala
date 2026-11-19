package Micro.Actions.Combat.Decisioning.Kiting

/**
 * Melee kite (no game access): the ranged KiteAway answers when, this
 * one answers whether melee yields ground at all. A blade line never
 * kites on cooldown alone the way guns do - standing trades while
 * running donates free hits to faster chasers. It yields only when
 * the duel is already lost: slow Zerglings in reach (they surround a
 * standing blade but cannot catch a yielding one), more enemy Zealots
 * than us around, or a hull under a third health still swinging at
 * melee. Upgraded speedlings are never yielded to: outrun and outnumbered,
 * the blade stands and swings instead of donating its back. Pure flags
 * for the suite; the behavior reads the band and the duel.
 */
object MeleeKiteAway {

  /** A hull at or below this many hit points is fighting on borrowed time. */
  val woundHp: Int = 60

  /**
   * Blades need no formation to yield: melee never holds formation
   * slots (the dress line poses ranged hulls, blades close), so a
   * formation gate would leave blades standing on formation-less maps
   * while lings surround them. Ranged hulls keep the gate - their kite
   * is a dress-line maneuver.
   */
  def requiresFormation(melee: Boolean): Boolean = ! melee

  def shouldRetreat(
    totalHp: Int,
    coolingDown: Boolean,
    hasSlowLingNearby: Boolean,
    outnumberedByZealots: Boolean,
    attackingMelee: Boolean): Boolean =
    coolingDown && (
      hasSlowLingNearby ||
        outnumberedByZealots ||
        (totalHp <= woundHp && attackingMelee))
}
