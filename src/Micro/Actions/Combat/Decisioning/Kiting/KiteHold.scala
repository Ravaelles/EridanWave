package Micro.Actions.Combat.Decisioning.Kiting

/**
 * Kite-hold discipline (owner direction: never yield ground for free):
 * a loaded-enough hull holds its volleys inside its own reach and only
 * yields what the cooldown forces. Pure numbers for the suite; the
 * behavior reads them at the kite leg. Near-dead hulls (shields
 * collapsed while still in contact) never plant - they kite early and
 * far instead, past the wound ladder that would hold them.
 */
object KiteHold {

  /** Plant window: the last cooldown frames worth holding for the volley. */
  var plantCooldownFrames: Int = 6

  /** Shields at or below this mean the hull is bleeding out. */
  var nearDeadShields: Int = 5

  /** Near-dead step: far, not wound-scaled - the hull gets out, not even. */
  var nearDeadOffsetTiles: Double = 2.8

  def isNearDead(shieldPoints: Int, maxShields: Int): Boolean =
    maxShields > 0 && shieldPoints <= nearDeadShields

  /**
   * Plant gate: something hittable is inside our reach right now, the
   * gun loads inside the window, and the hull is not near-dead. Then
   * holding the volley outdamages any ground the yield would buy.
   */
  /**
   * Plant gate: something hittable is inside our reach right now, the gun
   * loads inside the window, the hull is scratched but not near-dead,
   * and no blades are on top. The plant is a kite-family hold, so only
   * wounded hulls plant - healthy hulls belong to the line (dress and
   * approach), never to the duel. Blades carve a planted hull, so the
   * plant holds volleys against guns, never against contact. Near-dead
   * hulls never plant - only yield.
   */
  def shouldPlant(cooldownLeft: Int, hasHittable: Boolean, shieldPoints: Int, maxShields: Int, hasMeleeNearby: Boolean): Boolean =
    hasHittable && ! hasMeleeNearby && ! isNearDead(shieldPoints, maxShields) && isWounded(shieldPoints, maxShields) && cooldownLeft <= plantCooldownFrames

  /** Any shield scratch: the line owns whole hulls, the duel owns marked ones. */
  def isWounded(shieldPoints: Int, maxShields: Int): Boolean =
    maxShields > 0 && shieldPoints < maxShields

  /**
   * Near-dead flight gate: collapsed shields with melee in contact kite
   * on contact alone - blades carve a planted hull, and the wound ladder
   * below would hold a dying hull for shots it never lives to fire. The
   * melee condition is load-bearing: equal-speed ranged guns cannot be
   * outrun, so against guns alone the hull holds its last volleys
   * instead of donating them. Out of contact the margin gate releases
   * it; shieldless hulls never qualify.
   */
  def shouldFleeNearDead(shieldPoints: Int, maxShields: Int, enemyInMargin: Boolean, hasMeleeNearby: Boolean): Boolean =
    enemyInMargin && hasMeleeNearby && isNearDead(shieldPoints, maxShields)
}
