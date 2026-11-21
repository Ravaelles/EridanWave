package Tactic.Squads.Decisions

import Lifecycle.PerGame

import scala.collection.mutable

/**
 * Tank verdict (pure numbers, no game access): one object owning the
 * whole question of sieged guns - flee them or fight them. Inside
 * their reach ground warriors withdraw unconditionally (standing or
 * closing there donates shells before the first one even lands); a
 * dead-zone hull shoots for free and a cooling hug walks the tank down
 * instead. Past the reach the standoff band decides by superiority:
 * commit only with a stronger local AND global army, otherwise leave -
 * half-retreating around the edge loses the hull either way, so the
 * verdict never parks there. Past the band the duel has not started
 * and the verdict abstains. The ratio latch (enter at two-to-one,
 * release below three-to-two, both fronts must agree) owns the
 * handover, so one decisive attempt replaces the in-out dither; the
 * owner persists past the band instead of renegotiating it. FIGHT
 * falls through to the normal ladder (close and duel); LEAVE walks
 * out through the retreat plans. Keyed by unit id and swept per game
 * like the retreat and motion buses.
 */
object TankVerdict extends PerGame {

  /** What the verdict orders about the guns this frame. */
  sealed trait Call

  /** Close and duel: the odds or the dead zone say fight. */
  case object Fight extends Call

  /** Walk out: outranged and outmassed, no parking under fire. */
  case object Leave extends Call

  /** Standoff past our reach plus this, in tiles: the band decides by odds. */
  val standoffBandTiles: Double = 2.0

  /** Commit a stronger army at this local and global ratio. */
  val fightEnterRatio: Double = 2.0

  /** Abandon below this local or global ratio once committed. */
  val fightReleaseRatio: Double = 1.5

  def odds(us: Double, foe: Double): Double =
    if (foe <= 0) Double.PositiveInfinity else us / foe

  /**
   * Verdict desire (pure numbers for the suite): dead zone and hug
   * fight outright; past reach plus band the duel has not started;
   * between them the latched odds decide - enter at two-to-one on
   * both fronts, release below three-to-two on either. No enemy at
   * all means no fear.
   */
  def desire(
    distTiles: Double,
    reachTiles: Double,
    minTiles: Double,
    hugNow: Boolean,
    localUs: Double,
    localFoe: Double,
    globalUs: Double,
    globalFoe: Double,
    fightingNow: Boolean): Option[Call] = {
    if (distTiles < minTiles || hugNow) return Some(Fight)
    if (distTiles > reachTiles + standoffBandTiles) return None

    val edge = Math.min(odds(localUs, localFoe), odds(globalUs, globalFoe))
    val superior = if (fightingNow) edge >= fightReleaseRatio else edge >= fightEnterRatio
    Some(if (superior) Fight else Leave)
  }

  /**
   * Verdict memory (pure flags for the suite): a fresh call always
   * wins, past the band the last call stands instead of
   * renegotiating the edge every frame.
   */
  def owner(current: Option[Call], desired: Option[Call]): Option[Call] =
    desired.orElse(current)

  private val verdict = new mutable.HashMap[Int, Call]()

  /** Bus tick: one call per reader per frame, the ladder's desire wins. */
  def update(id: Int, call: Call): Unit = {
    verdict(id) = call
  }

  /** Held verdict both phases obey; unknown units hold none. */
  def held(id: Int): Option[Call] =
    verdict.get(id)

  override def reset(): Unit = {
    verdict.clear()
  }
}
