package Micro.Actions.Combat.Decisioning

import Micro.Actions.Combat.Decisioning.Kiting.KiteAway
import org.scalatest.funsuite.AnyFunSuite

class KiteAwaySuite extends AnyFunSuite {

  test("tuning defaults are pinned: parametrization must not shift behavior silently") {
    assert(KiteAway.seriousWoundHp == 65)
    assert(KiteAway.criticalWoundHp == 25)
    assert(KiteAway.seriousCooldown == 10)
    assert(KiteAway.criticalCooldown == 4)
    assert(KiteAway.mildCooldown == 18)
    assert(KiteAway.kiteVsRangedHp == 61)
    assert(KiteAway.enemyMarginTiles == 1.8)
    assert(KiteAway.maxOffsetTiles == 1.4)
    assert(KiteAway.freshShieldWound == 14)
    assert(KiteAway.meleeCloseTiles == 2.6)
    assert(KiteAway.kiteCooldownFrames == 10)
    assert(KiteAway.kiteEdgeTiles == 0.7)
    assert(KiteAway.steadyCooldownFrames == 15)
    assert(KiteAway.steadyContactTiles == 2.7)
    assert(KiteAway.zergShieldWound == 8)
    assert(KiteAway.hideAwayCooldown == 6)
  }

  test("wound levels follow total HP boundaries") {    assert(KiteAway.isCriticallyWounded(25))
    assert( ! KiteAway.isCriticallyWounded(26))
    assert(KiteAway.isSeriouslyWounded(65))
    assert(KiteAway.isSeriouslyWounded(25))
    assert( ! KiteAway.isSeriouslyWounded(66))
  }

  test("offset grows linearly with injury up to the maximum") {
    assert(KiteAway.woundOffsetTiles(0.0) == 0.0)
    assert(KiteAway.woundOffsetTiles(0.5) == KiteAway.maxOffsetTiles / 2)
    assert(KiteAway.woundOffsetTiles(1.0) == KiteAway.maxOffsetTiles)
  }

  private def retreat(
    totalHealth: Int,
    cooldownLeft: Int,
    enemyInMargin: Boolean = true,
    hasMeleeNearby: Boolean = false): Boolean =
    KiteAway.shouldRetreat(totalHealth, cooldownLeft, enemyInMargin, hasMeleeNearby)

  test("critical retreats until nearly ready, serious until almost ready") {
    assert(retreat(totalHealth = 20, cooldownLeft = 5))
    assert( ! retreat(totalHealth = 20, cooldownLeft = 4))
    assert(retreat(totalHealth = 60, cooldownLeft = 11))
    assert( ! retreat(totalHealth = 60, cooldownLeft = 10))
  }

  test("mild wounds hold against ranged guns: only the dying yield") {    assert( ! retreat(totalHealth = 120, cooldownLeft = 16))
    assert( ! retreat(totalHealth = 120, cooldownLeft = 22))
    assert( ! retreat(totalHealth = 170, cooldownLeft = 22))
  }

  test("no retreat without an enemy pressing with margin") {
    assert( ! retreat(totalHealth = 20, cooldownLeft = 22, enemyInMargin = false))
  }

  test("melee on top always kites while cooling, at any health") {
    assert(retreat(totalHealth = 180, cooldownLeft = 20, hasMeleeNearby = true))
    assert( ! retreat(totalHealth = 180, cooldownLeft = 15, hasMeleeNearby = true))
    assert( ! retreat(totalHealth = 180, cooldownLeft = 0, hasMeleeNearby = true))
  }

  test("fresh hulls hold the line without melee around") {
    assert( ! retreat(totalHealth = 170, cooldownLeft = 22))
    assert( ! retreat(totalHealth = 166, cooldownLeft = 22))
    assert( ! retreat(totalHealth = 165, cooldownLeft = 22))
  }

  test("sub-61 hulls save themselves, everything else holds and shoots") {
    assert(retreat(totalHealth = 60, cooldownLeft = 11))
    assert( ! retreat(totalHealth = 60, cooldownLeft = 10))
    assert( ! retreat(totalHealth = 61, cooldownLeft = 22))
    assert( ! retreat(totalHealth = 140, cooldownLeft = 22))
    assert( ! retreat(totalHealth = 170, cooldownLeft = 22))
  }

  test("seriously wounded skip volley discipline") {
    assert(KiteAway.exemptFromVolley(60))
    assert(KiteAway.exemptFromVolley(20))
    assert( ! KiteAway.exemptFromVolley(66))
  }

  test("a banked stock skips kiting, poverty kites") {
    assert( ! KiteAway.skipKiting(0))
    assert( ! KiteAway.skipKiting(999))
    assert(KiteAway.skipKiting(1000))
    assert(KiteAway.skipKiting(4000))
  }

  test("fresh hulls and ranged-only pressure never kite") {
    assert(KiteAway.shouldKite(shieldWound = 15, meleeNearby = true))
    assert(KiteAway.shouldKite(shieldWound = 80, meleeNearby = true))
    assert( ! KiteAway.shouldKite(shieldWound = 14, meleeNearby = true))
    assert( ! KiteAway.shouldKite(shieldWound = 0, meleeNearby = true))
    assert( ! KiteAway.shouldKite(shieldWound = 15, meleeNearby = false))
    assert( ! KiteAway.shouldKite(shieldWound = 80, meleeNearby = false))
  }

  test("fresh, loaded and clear hulls hold their fire instead of kiting") {
    assert(KiteAway.holdsFire(cooldownLeft = 0, hitPoints = 100, enemyClose = false))
    assert(KiteAway.holdsFire(cooldownLeft = 15, hitPoints = 80, enemyClose = false))
    assert( ! KiteAway.holdsFire(cooldownLeft = 16, hitPoints = 80, enemyClose = false))
    assert( ! KiteAway.holdsFire(cooldownLeft = 0, hitPoints = 79, enemyClose = false))
    assert( ! KiteAway.holdsFire(cooldownLeft = 0, hitPoints = 80, enemyClose = true))
  }

  test("cooling-but-healthy hulls hold their volleys in range") {
    assert(KiteAway.holdsVolley(cooldownLeft = 20, totalHealth = 81))
    assert(KiteAway.holdsVolley(cooldownLeft = 30, totalHealth = 100))
    assert( ! KiteAway.holdsVolley(cooldownLeft = 19, totalHealth = 100))
    assert( ! KiteAway.holdsVolley(cooldownLeft = 20, totalHealth = 80))
    assert( ! KiteAway.holdsVolley(cooldownLeft = 0, totalHealth = 100))
  }

  test("contact floor never drops below the paced step") {
    assert(KiteAway.contactYield(woundWantPixels = 0.0, pacedPixels = 96.0) == 96.0)
    assert(KiteAway.contactYield(woundWantPixels = 120.0, pacedPixels = 96.0) == 120.0)
    assert(KiteAway.contactYield(woundWantPixels = 96.0, pacedPixels = 96.0) == 96.0)
  }

  test("cooling guns hold their volleys, loaded guns hold the edge") {
    assert(KiteAway.holdsForVolley(cooldownLeft = 0))
    assert(KiteAway.holdsForVolley(cooldownLeft = 10))
    assert( ! KiteAway.holdsForVolley(cooldownLeft = 11))
    assert(KiteAway.holdsTheEdge(distTiles = 3.3, rangeTiles = 4.0))
    assert(KiteAway.holdsTheEdge(distTiles = 4.0, rangeTiles = 4.0))
    assert( ! KiteAway.holdsTheEdge(distTiles = 3.2, rangeTiles = 4.0))
  }

  test("edge-capped steps never walk out of range") {
    assert(math.abs(KiteAway.edgeCappedYield(stepPixels = 128.0, distTiles = 2.0, rangeTiles = 4.0, edgeTiles = 0.7) - 1.3 * 32.0) < 1e-9)
    assert(math.abs(KiteAway.edgeCappedYield(stepPixels = 128.0, distTiles = 3.0, rangeTiles = 4.0, edgeTiles = 0.7) - 0.3 * 32.0) < 1e-9)
    assert(KiteAway.edgeCappedYield(stepPixels = 128.0, distTiles = 3.5, rangeTiles = 4.0, edgeTiles = 0.7) == 0.0)
  }

  test("pins the lone-blade chase, leash and escape") {
    assert(KiteAway.chaseTiles == 7.0)
    assert(KiteAway.leashTiles == 6.0)
    assert(KiteAway.meleeEscapeTiles == 1.0)
    assert(KiteAway.kiteCommitTiles == 6.0)
    assert(KiteAway.steadyHoldFrames == 8)
  }

  test("an approaching blade starts the kite before contact") {
    assert(KiteAway.shouldChase(distTiles = 7.0, chaseTiles = 7.0))
    assert(KiteAway.shouldChase(distTiles = 3.0, chaseTiles = 7.0))
    assert( ! KiteAway.shouldChase(distTiles = 7.1, chaseTiles = 7.0))
  }

  test("a lone blade with no guns nearby belongs to the duel, not the line") {
    assert(KiteAway.loneBladeDuel(gunsNear = false, meleeChase = true, melee = false))
    assert( ! KiteAway.loneBladeDuel(gunsNear = true, meleeChase = true, melee = false))
    assert( ! KiteAway.loneBladeDuel(gunsNear = false, meleeChase = false, melee = false))
    assert( ! KiteAway.loneBladeDuel(gunsNear = false, meleeChase = true, melee = true))
  }

  test("unoutrunnable pressers are traded, slower ones are kited to the edge") {
    assert(KiteAway.shouldTradeContact(unitTopSpeed = 5.0, fastestBladeSpeed = 6.0))
    assert(KiteAway.shouldTradeContact(unitTopSpeed = 5.0, fastestBladeSpeed = 5.0))
    assert( ! KiteAway.shouldTradeContact(unitTopSpeed = 5.0, fastestBladeSpeed = 4.0))
    assert( ! KiteAway.shouldTradeContact(unitTopSpeed = 5.0, fastestBladeSpeed = 0.0))
  }

  test("the duel owns contact, hittable guns and seen enemies in range") {
    assert(KiteAway.duelInReach(hasContact = true, hasHittable = false, seenNear = false))
    assert(KiteAway.duelInReach(hasContact = false, hasHittable = true, seenNear = false))
    assert(KiteAway.duelInReach(hasContact = false, hasHittable = false, seenNear = true))
    assert( ! KiteAway.duelInReach(hasContact = false, hasHittable = false, seenNear = false))
  }

  test("a cooling dragoon with blades on top always yields, loaded or fresh") {
    assert(KiteAway.mustFleeContact(isDragoon = true, cooldownLeft = 15, meleeContact = true, totalHealth = 54))
    assert(KiteAway.mustFleeContact(isDragoon = true, cooldownLeft = 30, meleeContact = true, totalHealth = 40))
    assert( ! KiteAway.mustFleeContact(isDragoon = true, cooldownLeft = 15, meleeContact = true, totalHealth = 55))
    assert( ! KiteAway.mustFleeContact(isDragoon = true, cooldownLeft = 15, meleeContact = true, totalHealth = 100))
    assert( ! KiteAway.mustFleeContact(isDragoon = true, cooldownLeft = 14, meleeContact = true, totalHealth = 40))
    assert( ! KiteAway.mustFleeContact(isDragoon = true, cooldownLeft = 15, meleeContact = false, totalHealth = 40))
    assert( ! KiteAway.mustFleeContact(isDragoon = false, cooldownLeft = 15, meleeContact = true, totalHealth = 40))
  }

  test("a held heading survives jitter but releases on turns and lapses") {
    assert( ! KiteAway.shouldRefreshHeading(lastFrame = 10, frame = 12))
    assert(KiteAway.shouldRefreshHeading(lastFrame = 10, frame = 18))
    assert(KiteAway.shouldRefreshHeading(lastFrame = 100, frame = 5))
    assert(KiteAway.holdsHeading(freshRadians = 0.1, heldRadians = 0.0, coneRadians = math.Pi / 4))
    assert( ! KiteAway.holdsHeading(freshRadians = 1.0, heldRadians = 0.0, coneRadians = math.Pi / 4))
  }

  test("the leash pulls home but never toward the chaser") {
    assert(KiteAway.shouldLeash(distFriendTiles = 6.1, leashTiles = 6.0, distFriendFoePx = 400.0, distUnitFoePx = 100.0, alignedAway = true))
    assert( ! KiteAway.shouldLeash(distFriendTiles = 6.0, leashTiles = 6.0, distFriendFoePx = 400.0, distUnitFoePx = 100.0, alignedAway = true))
    assert( ! KiteAway.shouldLeash(distFriendTiles = 10.0, leashTiles = 6.0, distFriendFoePx = 100.0, distUnitFoePx = 100.0, alignedAway = true))
    assert( ! KiteAway.shouldLeash(distFriendTiles = 10.0, leashTiles = 6.0, distFriendFoePx = 50.0, distUnitFoePx = 100.0, alignedAway = true))
    assert( ! KiteAway.shouldLeash(distFriendTiles = 10.0, leashTiles = 6.0, distFriendFoePx = 400.0, distUnitFoePx = 100.0, alignedAway = false))
  }

  test("the run home must head away from the foe") {
    val unit = new Mathematics.Points.Pixel(0, 0)
    val north = new Mathematics.Points.Pixel(0, 100)
    val foeSouth = new Mathematics.Points.Pixel(0, -100)
    assert(KiteAway.alignedAway(unit, north, foeSouth))
    assert( ! KiteAway.alignedAway(unit, foeSouth, foeSouth))
    assert( ! KiteAway.alignedAway(unit, new Mathematics.Points.Pixel(100, 0), foeSouth))
  }

  test("a loaded hull with blades on top still steps out") {
    assert(KiteAway.escapeYield(stepPixels = 0.0, escapeTiles = 1.0) == 32.0)
    assert(KiteAway.escapeYield(stepPixels = 64.0, escapeTiles = 1.0) == 64.0)
  }

  test("a cooling dragoon always hides from templars, cooldown six and up") {
    assert(KiteAway.mustHideFromTemplar(isDragoon = true, cooldownLeft = 6, templarNear = true))
    assert(KiteAway.mustHideFromTemplar(isDragoon = true, cooldownLeft = 30, templarNear = true))
    assert( ! KiteAway.mustHideFromTemplar(isDragoon = true, cooldownLeft = 5, templarNear = true))
    assert( ! KiteAway.mustHideFromTemplar(isDragoon = true, cooldownLeft = 30, templarNear = false))
    assert( ! KiteAway.mustHideFromTemplar(isDragoon = false, cooldownLeft = 30, templarNear = true))
  }
}
