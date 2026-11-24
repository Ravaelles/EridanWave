package Micro.Actions.Combat.Decisioning.Kiting

/**
 * Dragoon-versus-slowlot duel (owner order, PurpleWave road): one
 * matchup, one decision-maker. A cooling dragoon keeps a
 * hit-point-scaled margin from slow blades - approach to firing range
 * loaded, walk back cooling, retreat only when deep. The bloodier the
 * hull, the more ground the margin buys: fresh hulls stand their
 * shots, dying hulls yield early and far. Pure numbers; the duel leg
 * beside it owns every order.
 */
object DragoonVsSlowlot {

  /** Threats are sensed this far out, in pixels. */
  val sensePixels: Double = 160.0

  /** Firing-position slack, in pixels: attack from here, not on top. */
  val fireMarginPixels: Double = 32.0

  /** Shots of slack before the next hit kills us. */
  def hitsLeft(totalHealth: Int): Int =
    totalHealth / 14

  /**
   * One threat's margin contribution, in pixels: how far we stand from
   * opening fire, padded when it already aims at us, shortened when it
   * walks away from us.
   */
  def threatMarginPixels(pixelsToGetInRange: Double, targeted: Boolean, approachingDot: Double): Double =
    pixelsToGetInRange + fireMarginPixels - (if (targeted) 16.0 else 0.0) - 16.0 * approachingDot

  /** Tiles of slack plus shots of slack: how safe this margin is. */
  def safety(marginPixels: Int, totalHealth: Int): Int =
    marginPixels / 32 + hitsLeft(totalHealth)

  /**
   * Priority follows safety (0 shove .. 3 pardon): dying dragoons shove
   * through the line, comfortable ones yield it. The leg maps the tier
   * to traffic - the kernel never touches an order.
   */
  def priorityTier(safety: Int): Int =
    if (safety < 3) 0
    else if (safety < 5) 1
    else if (safety < 7) 2
    else 3

  /** How many pixels of margin N shots of slack buy. */
  def marginRequiredPixels(totalHealth: Int): Int =
    Math.max(32, 128 - 8 * hitsLeft(totalHealth))

  /** Positive means hold, negative means run. */
  def marginDeltaPixels(marginPixels: Int, totalHealth: Int): Int =
    marginPixels - marginRequiredPixels(totalHealth)

  /** Deep inside our own reach: walk out instead of trading contact. */
  def insideOwnReach(pixelsToGetInRange: Double): Boolean =
    pixelsToGetInRange < -fireMarginPixels

  /** Loaded enough to step in: past this surplus the gun outdamages
   * the ground the step would buy. */
  val approachSurplusPixels: Int = 32

  def shouldApproach(marginDeltaPixels: Int): Boolean =
    marginDeltaPixels > approachSurplusPixels
}
