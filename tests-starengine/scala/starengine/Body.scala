package starengine

import ProxyBwapi.UnitClasses.UnitClass

/**
 * A body in the StarEngine world: state only, never decisions. Speeds,
 * reaches, damage and cooldowns come straight from the unit's class
 * (UnitType tables - no hardcoded movement numbers, the one rule of this
 * engine). Pixels everywhere, like the game reports them. Simplifications,
 * all documented here: constant top speed with instant stops (no
 * acceleration or braking), one pooled hit-point bar (no shield/hull
 * split), instant damage on the firing frame (no projectiles, no scarab
 * flight), radial splash as full-plus-half (no divisor ladder), storms as
 * instant flat damage in the cast frame (no lingering field).
 */
final class Body(val kind: UnitClass, var x: Double, var y: Double, var hp: Double) {

  var cooldownLeft: Int = 0

  /**
   * Last frame this body took damage (-1 when never hit): the engine
   * stamps it in the gun phase, pilots read it through World's
   * underAttack query - the stand-in for BWAPI's sticky isUnderAttack
   * flag, which outlives the exact damage frame too.
   */
  var lastDamageFrame: Int = -1

  /** Spell energy (High Templars start scenarios charged, everyone else at zero). */
  var energy: Int = 0

  /**
   * Passive hulls never shoot: decoys that draw fire for the Reaver to
   * clear, mirroring hold-fire discipline. They still move, take damage
   * and count as targets.
   */
  var passive: Boolean = false

  /**
   * Cloaked hulls cannot be shot without detection (mirrors cloak plus
   * detector coverage): guns skip them unless a live foe detector's
   * sight reaches them (see World's detectorsRevealing). Off by default,
   * so scenarios without the concept behave exactly as before.
   */
  var cloaked: Boolean = false

  /**
   * Transport carrying this body (loaded cargo rides, never shoots and
   * cannot be hit - BW-faithful, like the engine's other documented
   * simplifications). Point bodies board by center distance; see
   * World.BoardRangePx.
   */
  var transport: Option[Body] = None

  def loaded: Boolean = transport.isDefined

  def alive: Boolean = hp > 0

  def maxHp: Double = kind.maxTotalHealth

  def lossFraction: Double = (maxHp - hp) / maxHp

  def speed: Double = kind.topSpeed

  def rangeAgainst(foe: Body): Double =
    if (foe.kind.isFlyer) kind.pixelRangeAir else kind.pixelRangeGround

  def damageAgainst(foe: Body): Int =
    if (foe.kind.isFlyer) kind.effectiveAirDamage else kind.effectiveGroundDamage

  def cooldownAgainst(foe: Body): Int =
    if (foe.kind.isFlyer) kind.airDamageCooldown else kind.groundDamageCooldown

  def canHit(foe: Body): Boolean =
    foe.alive && damageAgainst(foe) > 0 && rangeAgainst(foe) > 0

  /**
   * Outer splash radius, in pixels (real BWAPI weapon data, Scarab for the
   * Reaver); 0 without ground radial splash. No scarab flight is modeled -
   * the shot lands instantly, per the approximation contract.
   */
  def splashOuterPx: Double = kind.splashOuterPx

  def dist(o: Body): Double = {
    val dx = x - o.x
    val dy = y - o.y
    Math.sqrt(dx * dx + dy * dy)
  }

  def stepToward(tx: Double, ty: Double): Unit = {
    val dx = tx - x
    val dy = ty - y
    val dist = Math.sqrt(dx * dx + dy * dy)
    if (dist > 0) {
      val stride = Math.min(speed, dist)
      x += dx / dist * stride
      y += dy / dist * stride
    }
  }
}
