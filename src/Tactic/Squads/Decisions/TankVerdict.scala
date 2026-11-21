package Tactic.Squads.Decisions

import Lifecycle.PerGame

import scala.collection.mutable

/**
 * Tank verdict (pure numbers, no game access): one object owning the
 * whole question of tanks - flee them or fight them. Inside their
 * reach ground warriors withdraw unconditionally, unless a dead-zone
 * hull shoots for free or a cooling hug walks the tank down. Past the
 * reach the standoff band evaluates who stands near the guns: enemies
 * that can answer us inside escort range of the tank against our
 * warriors nearby. A lonely tank always loses to the dead-zone walk,
 * so it is fought on sight; an escorted one takes two-to-one odds to
 * commit and drops the push below three-to-two on either side. Past
 * the band the duel has not started and the verdict abstains, keeping
 * the last call instead of renegotiating the edge every frame. FIGHT
 * closes to point-blank and duels; LEAVE walks out through Fallback.
 * Keyed by unit id and swept per game like the retreat verdict.
 */
object TankVerdict extends PerGame {

  /** What the verdict orders about the guns this frame. */
  sealed trait Call

  /** Close to point-blank and duel: the odds or the dead zone say fight. */
  case object Fight extends Call

  /** Walk out: outranged and outmassed, no parking under fire. */
  case object Leave extends Call

  /** Standoff past the reach, in tiles: the band evaluates the escorts. */
  val standoffBandTiles: Double = 2.0

  /** Enemies this far from the tank, in tiles, still cover it. */
  val escortRadiusTiles: Double = 14.0

  /** Our warriors this far from us, in tiles, still fight with us. */
  val packRadiusTiles: Double = 12.0

  /** Commit a stronger force at this local ratio. */
  val fightEnterRatio: Double = 2.0

  /** Abandon the push below this local ratio once committed. */
  val fightReleaseRatio: Double = 1.5

  def odds(us: Double, foe: Double): Double =
    if (foe <= 0.0) Double.PositiveInfinity else us / foe

  /**
   * Verdict desire (pure numbers for the suite): dead-zone hulls and
   * cooling hugs fight outright; past reach plus band the duel has not
   * started; a lonely tank is always walked down; an escorted one
   * needs two-to-one to commit and drops the push below
   * three-to-two. Non-duelists abstain - the keep-off still covers
   * them. No enemy near the guns means no fear.
   */
  def desire(
    distTiles: Double,
    reachTiles: Double,
    minTiles: Double,
    hugNow: Boolean,
    canDuel: Boolean,
    lonely: Boolean,
    usSkim: Double,
    foeSkim: Double,
    fightingNow: Boolean): Option[Call] = {
    if ( ! canDuel) return None

    val superior = {
      val edge = odds(usSkim, foeSkim)
      if (fightingNow) edge >= fightReleaseRatio else edge >= fightEnterRatio
    }
    // Dead zone and hugged duels are free kills - but only walked into
    // alone or ahead: cooling into covered guns donates the hull that
    // the withdrawal was saving.
    if (distTiles < minTiles) return Some(Fight)

    if (hugNow && (lonely || superior)) return Some(Fight)

    if (distTiles > reachTiles + standoffBandTiles) return None

    if (lonely) return Some(Fight)

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

  /** Clear the verdict: no guns left, no grudge held. */
  def clear(id: Int): Unit = {
    verdict.remove(id)
  }

  /** Held verdict both readers obey; unknown units hold none. */
  def held(id: Int): Option[Call] =
    verdict.get(id)

  override def reset(): Unit = {
    verdict.clear()
  }
}
