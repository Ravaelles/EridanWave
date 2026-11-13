package Micro.Actions.Combat.Decisioning.Kiting

/**
 * Pure hold-to-shoot race (no game access), ported from Atlantis
 * ProtossAttackHoldToShoot: the nominal range is not the range at which a
 * shot lands while both sides move. Braking glides us, orders take latency
 * frames, and the chaser keeps closing through all of it. The range that
 * matters is the EFFECTIVE one (R*): foe reach, plus what the chaser covers
 * during our windup, minus our braking glide. This answers the race given
 * R*: if the weapon is ready when the enemy reaches R* (with a hold margin
 * for order-issue slop), stand and wait (no wasted ground); else kite.
 *
 * Head-on scope: the caller passes the closing speed along the gap axis
 * (a committed chaser runs it at ~full speed straight at us). A runner
 * (closing <= 0) never holds. Physics stays in pixels inside; the seam
 * converts once. Knobs are public vars, KiteAway style, for tuning.
 */
object HoldToShoot {

  /** Extra frames of arrival lead required to stand (order-issue slop). */
  var holdMarginFrames: Int = 5

  /** Cooldown at or below which a Dragoon fires instead of kiting. */
  var attackCooldownFrames: Int = 5

  /** Head-on reach multiplier: a committed Vulture eats the shot early. */
  var approachBonus: Double = 1.5

  /** Potshot reach past our own range that still draws fire, in tiles. */
  var potshotBonusTiles: Double = 1.5

  /**
   * Vulture potshot (pure numbers for the suite): with the gun nearly ready
   * and a Vulture closing head-on inside our reach plus the bonus, shoot
   * instead of picking another duel - the shot is free, the kite is not.
   * The caller feeds live gap, reach, cooldown and facing; this only
   * combines them.
   */
  def shouldPotshotVulture(
    distPx:        Double,
    ownRangePx:    Double,
    cooldownLeft:  Int,
    foeFacingUs:   Boolean): Boolean =
    foeFacingUs &&
      cooldownLeft <= attackCooldownFrames &&
      distPx <= ownRangePx + potshotBonusTiles * 32.0

  /**
   * Vulture duel (pure numbers for the suite): with the gun nearly ready
   * and a Vulture in range, shoot it instead of yielding ground; a Vulture
   * sprinting straight at us meets the shot already at bonus reach, so the
   * kite never donates free distance. The caller feeds live gap, reach,
   * cooldown, closing speed and facing; this only combines them.
   */
  def shouldStandVsVulture(
    distPx:             Double,
    rangePx:            Double,
    cooldownLeft:       Int,
    latencyFrames:      Int,
    closingPxPerFrame:  Double,
    foeFacingUs:        Boolean): Boolean = {
    if (distPx <= rangePx && cooldownLeft <= attackCooldownFrames) return true
    if ( ! foeFacingUs || closingPxPerFrame <= 0) return false

    shouldStand(
      distPx,
      approachBonus * rangePx,
      cooldownLeft,
      latencyFrames,
      closingPxPerFrame,
      holdMarginFrames)
  }

  /**
   * The effective range R*: foe reach, plus what the chaser covers during
   * our windup, minus our braking glide. Floored at zero: braking longer
   * than the whole exchange means the stop is free.
   */
  def effectiveRange(
    foeReachPx:           Double,
    foeSpeedPxPerFrame:   Double,
    windupFrames:         Int,
    brakingPx:            Double): Double =
    Math.max(0, foeReachPx + foeSpeedPxPerFrame * windupFrames - brakingPx)

  /**
   * True when the weapon is ready on enemy arrival: stand and wait.
   * Gap and range in pixels, times in frames.
   */
  def shouldStand(
    gapPx:              Double,
    effectiveRangePx:   Double,
    cooldownLeft:       Int,
    latencyFrames:      Int,
    closingPxPerFrame:  Double,
    holdMarginFrames:   Int): Boolean = {
    if (closingPxPerFrame <= 0) return false

    val reachIn = (gapPx - effectiveRangePx) / closingPxPerFrame
    val readyIn = cooldownLeft + latencyFrames

    if (reachIn <= 0) return readyIn <= 0

    reachIn >= readyIn + holdMarginFrames
  }
}
