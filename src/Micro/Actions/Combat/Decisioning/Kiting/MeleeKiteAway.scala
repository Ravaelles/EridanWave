package Micro.Actions.Combat.Decisioning.Kiting

/**
 * Melee kite (no game access): the ranged KiteAway answers when, this
 * one answers whether melee yields ground at all. A blade line never
 * kites on cooldown alone the way guns do - standing trades while
 * running donates free hits to faster chasers. It yields only when
 * the duel is already lost: slow Zerglings in reach (they surround a
 * standing blade but cannot catch a yielding one), more enemy Zealots
 * than us around, or a hull under a third health still swinging at
 * melee. Upgraded speedlings are only yielded to as a pack: alone a
 * blade cannot outrun them, so it stands and swings instead of
 * donating its back - but a balled line kites together, sharing the
 * beating instead of splitting apart. Pure flags for the suite; the
 * behavior reads the band and the duel.
 */
object MeleeKiteAway {

  /** A hull at or below this many hit points is fighting on borrowed time. */
  val woundHp: Int = 60

  /**
   * Kite with speedlings only inside this of a zealot mate, in tiles:
   * shoulder-to-shoulder is the tightest a ball gets, so this is the
   * intact-pack band - wider means the line already split.
   */
  val packKiteTiles: Double = 1.0

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
    hasSpeedlingNearby: Boolean,
    packIntact: Boolean,
    outnumberedByZealots: Boolean,
    attackingMelee: Boolean): Boolean =
    coolingDown && (
      hasSlowLingNearby ||
        (hasSpeedlingNearby && packIntact) ||
        outnumberedByZealots ||
        (totalHp <= woundHp && attackingMelee))

  /**
   * Calm blades hold: not hit in forty frames with 41+ HP means no
   * pressure worth yielding to - stand and swing instead of donating
   * ground (and the volley) for nothing. Owner order from the ZealDrag
   * mirror. Pure numbers for the suite; the behavior reads the clock.
   */
  val calmFrames: Int = 40

  val calmHp: Int = 41

  def holdsWhenCalm(framesSinceDamage: Int, totalHp: Int): Boolean =
    framesSinceDamage > calmFrames && totalHp > calmHp

  /**
   * Pack-average hold: blades whose mates average less HP hold the frame
   * instead of kiting - the healthier hull stands while the pack chews
   * together, instead of every blade yielding on its own wound and the
   * pack dissolving into a rout. PvP only (below); needs mates to
   * compare against.
   */
  /**
   * Pack-mate radius, in tiles: only shoulder-to-shoulder blades count
   * for the average - the pack that kites together must stand together.
   */
  val packMateTiles: Double = 2.0

  def standsWithWeakerPack(mateHpTotal: Int, mateCount: Int, myHp: Int): Boolean =
    mateCount > 0 && mateHpTotal.toDouble / mateCount < myHp
}
