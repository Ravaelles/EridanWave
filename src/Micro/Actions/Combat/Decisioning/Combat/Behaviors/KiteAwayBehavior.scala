package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Kiting.{ClusterSafety, FacingAway, HoldToShoot, KiteAway}
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Kite behavior (was CombatEngagement.kiteAway): wounded units yield ground
 * instead of dying in place. Runs before the dress line (withdrawing beats
 * dressing), only while an enemy presses within range plus margin and the
 * weapon still needs its cooldown. A committed chaser met with a ready gun
 * holds to shoot instead (HoldToShoot); turned-away melee never triggers
 * the retreat (FacingAway). Returns true when it moved or held the unit.
 */
object KiteAwayBehavior {

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if (With.yolo.active) return false
    // Committed units (scenario duelists, suicide charges, raid passengers)
    // are ordered to hold their ground: yielding it stops their fire while
    // the enemy keeps shooting, and the chase usually catches them anyway.
    if (unit.agent.commit) return false
    if ( ! unit.canMove) return false
    if (staticStandoff(unit, combat)) return true
    if ( ! kitable(unit)) return false

    val nearest = unit.matchups.targetNearest
    if (nearest.isEmpty) return false
    if ( ! shouldKite(unit)) return false
    if (holdToShoot(unit, combat)) return true

    kiteAway(unit, combat)

    true
  }

  private def kitable(unit: FriendlyUnitInfo): Boolean = {
    if (unit.unitClass.melee) return false
    if (unit.formationEngage.isEmpty) return false

    true
  }

  private def shouldKite(unit: FriendlyUnitInfo): Boolean = {
    val squadUnits = unit.squad.map(_.units).getOrElse(Seq(unit))
    // Turned-away melee neither chases nor hits: it never counts as the
    // enemy in margin that triggers the retreat.
    val enemyInMargin = squadUnits.exists(u =>
      u.matchups.threatsInPixels(KiteAway.enemyMarginTiles * 32).exists(t =>
        ! FacingAway.dismissMelee(t.unitClass.melee,
          FacingAway.showingBack(t.angleRadians, t.pixel.radiansTo(u.pixel)))))
    if ( ! KiteAway.shouldRetreat(unit.totalHealth, unit.woundPercent(), unit.cooldownLeft, enemyInMargin)) return false

    true
  }

  // Hold-to-shoot: a committed chaser met with a ready gun is a free shot,
  // not a reason to yield ground. Fires only when we'd otherwise kite, and
  // never steals another duel's target: the chaser must be ours or none.
  private def holdingShot(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    val chaser = Maff.minBy(unit.matchups.threats)(_.pixelDistanceEdge(unit))
    chaser.exists(c => combat.target.forall(_.id == c.id) && HoldToShoot.shouldStand(
      unit.pixelDistanceEdge(c),
      HoldToShoot.effectiveRange(c.pixelRangeAgainst(unit), c.topSpeed, windupFrames = 0, unit.topSpeed * unit.unitClass.accelerationFrames / 2.0),
      unit.cooldownLeft,
      With.latency.latencyFrames,
      c.speedApproaching(unit.pixel),
      HoldToShoot.holdMarginFrames))
  }

  private def holdToShoot(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! holdingShot(unit, combat)) return false

    unit.agent.act("HoldToShoot")
    unit.agent.decision.set(unit.pixel)
    Commander.hold(unit)

    true
  }

  private def kiteAway(unit: FriendlyUnitInfo, combat: Combat): Unit = {
    unit.agent.act("KiteAway")
    val offsetPixels = KiteAway.woundOffsetTiles(unit.woundPercent()) * 32
    unit.agent.decision.set(unit.pixel.project(unit.matchups.targetNearest.get.pixel, -offsetPixels).walkablePixel)
    combat.maneuvers.move()
  }

  // Static standoff: an idle unit standing inside a defensive building's
  // reach while the cluster takes fire steps out to range + margin + bonus
  // instead of absorbing hits. Fighting units (targets in range) and
  // committed pushes (shouldFight) are untouched; the attack window lapses
  // once fire stops, so the squad resumes on its own (hysteresis).
  private def staticStandoff(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! standoffHot(unit)) return false

    val gap = standoffGap(unit)
    if (gap <= 0) return false

    stepOut(unit, combat, gap)

    true
  }

  private def standoffHot(unit: FriendlyUnitInfo): Boolean = {
    if (unit.agent.shouldFight) return false

    if (unit.matchups.targetsInRange.nonEmpty) return false

    val squadUnits = unit.squad.map(_.units).getOrElse(Seq(unit))
    val clusterHot = squadUnits.exists(u => ClusterSafety.recentlyAttacked(With.framesSince(u.lastFrameTakingDamage)))
    if ( ! clusterHot) return false

    true
  }

  private def standoffGap(unit: FriendlyUnitInfo): Double = {
    val threat = Maff.minBy(unit.matchups.threats.filter(_.unitClass.isStaticDefense))(_.pixelDistanceEdge(unit))
    threat.map(t => ClusterSafety.standoffGapPixels(
      unit.pixelDistanceEdge(t),
      t.pixelRangeAgainst(unit),
      KiteAway.enemyMarginTiles,
      clusterAttacked = true)).getOrElse(0.0)
  }

  private def stepOut(unit: FriendlyUnitInfo, combat: Combat, gap: Double): Unit = {
    val threat = Maff.minBy(unit.matchups.threats.filter(_.unitClass.isStaticDefense))(_.pixelDistanceEdge(unit))
    unit.agent.act("StaticStandoff")
    unit.agent.decision.set(unit.pixel.project(threat.get.pixel, -gap).walkablePixel)
    combat.maneuvers.move()
  }
}
