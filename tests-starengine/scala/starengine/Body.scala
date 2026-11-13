package starengine

import ProxyBwapi.UnitClasses.UnitClass

/**
 * A body in the StarEngine world: state only, never decisions. Speeds,
 * reaches, damage and cooldowns come straight from the unit's class
 * (UnitType tables - no hardcoded movement numbers, the one rule of this
 * engine). Pixels everywhere, like the game reports them. Simplifications,
 * all documented here: constant top speed with instant stops (no
 * acceleration or braking), one pooled hit-point bar (no shield/hull
 * split), instant damage on the firing frame (no projectiles), storms as
 * instant flat damage in the cast frame (no lingering field).
 */
final class Body(val kind: UnitClass, var x: Double, var y: Double, var hp: Double) {

  var cooldownLeft: Int = 0

  /** Spell energy (High Templars start scenarios charged, everyone else at zero). */
  var energy: Int = 0

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
