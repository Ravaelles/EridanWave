package Micro.Actions.Combat.Decisioning.Kiting

import org.scalatest.funsuite.AnyFunSuite

/**
 * Kite-hold discipline (owner direction: never yield ground for free):
 * a loaded-enough hull holds its volleys inside its own reach and only
 * yields what the cooldown forces. The plant stands the last cooldown
 * frames when something hittable is there; near-dead hulls (shields
 * collapsed) never plant - they kite early and far instead. Shieldless
 * hulls are never near-dead: without shields the rule cannot fire.
 */
class KiteHoldSuite extends AnyFunSuite {

  test("a nearly loaded gun plants for the volley") {
    assert(KiteHold.shouldPlant(cooldownLeft = 0, hasHittable = true, shieldPoints = 80, maxShields = 80, hasMeleeNearby = false))
    assert(KiteHold.shouldPlant(cooldownLeft = 6, hasHittable = true, shieldPoints = 80, maxShields = 80, hasMeleeNearby = false))
  }

  test("long cooldowns keep yielding, empty reach keeps moving") {
    assert( ! KiteHold.shouldPlant(cooldownLeft = 7, hasHittable = true, shieldPoints = 80, maxShields = 80, hasMeleeNearby = false))
    assert( ! KiteHold.shouldPlant(cooldownLeft = 0, hasHittable = false, shieldPoints = 80, maxShields = 80, hasMeleeNearby = false))
  }

  test("blades on top never plant") {
    assert( ! KiteHold.shouldPlant(cooldownLeft = 0, hasHittable = true, shieldPoints = 80, maxShields = 80, hasMeleeNearby = true))
  }

  test("near-dead hulls never plant") {
    assert( ! KiteHold.shouldPlant(cooldownLeft = 0, hasHittable = true, shieldPoints = 5, maxShields = 80, hasMeleeNearby = false))
    assert( ! KiteHold.shouldPlant(cooldownLeft = 0, hasHittable = true, shieldPoints = 0, maxShields = 80, hasMeleeNearby = false))
  }

  test("shieldless hulls are never near-dead") {
    assert( ! KiteHold.isNearDead(shieldPoints = 0, maxShields = 0))
    assert(KiteHold.shouldPlant(cooldownLeft = 0, hasHittable = true, shieldPoints = 0, maxShields = 0, hasMeleeNearby = false))
  }

  test("near-dead hulls kite on melee contact alone, past the wound ladder") {
    assert(KiteHold.shouldFleeNearDead(shieldPoints = 5, maxShields = 80, enemyInMargin = true, hasMeleeNearby = true))
    assert( ! KiteHold.shouldFleeNearDead(shieldPoints = 6, maxShields = 80, enemyInMargin = true, hasMeleeNearby = true))
    assert( ! KiteHold.shouldFleeNearDead(shieldPoints = 5, maxShields = 80, enemyInMargin = false, hasMeleeNearby = true))
    assert( ! KiteHold.shouldFleeNearDead(shieldPoints = 0, maxShields = 0, enemyInMargin = true, hasMeleeNearby = true))
  }

  test("near-dead hulls hold their last volleys against guns alone") {
    assert( ! KiteHold.shouldFleeNearDead(shieldPoints = 5, maxShields = 80, enemyInMargin = true, hasMeleeNearby = false))
  }
}
