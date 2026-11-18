package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.{Maff, WoundMargin}
import Micro.Actions.Combat.Decisioning.Kiting.{ClusterSafety, FacingAway, HoldToShoot, KiteAway, MeleeKiteAway}
import Micro.Agency.Commander
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

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
    // A banked stock buys the trade: expendable units hold their DPS
    // instead of yielding ground while cooling down.
    if (KiteAway.skipKiting(With.self.minerals)) return false
    // Committed units (scenario duelists, raid passengers) hold their ground:
    // yielding it stops their fire while the enemy keeps shooting, and the
    // chase usually catches them anyway. Suicide missions still kite: a unit
    // inside the enemy base never retreats, but stepping out of range while
    // cooling down is dueling, not fleeing.
    if (unit.agent.commit && ! unit.isSuicideMission) return false
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
    if (unit.unitClass.melee && ! unit.isAny(Protoss.Zealot, Protoss.Archon)) return false
    // Blades need no formation to yield (MeleeKiteAway.requiresFormation):
    // melee never holds dress slots, so the gate would only strand blades
    // on formation-less maps. Ranged hulls keep it - their kite is a
    // dress-line maneuver.
    if (MeleeKiteAway.requiresFormation(unit.unitClass.melee) && unit.formationEngage.isEmpty) return false

    true
  }

  private def shouldKite(unit: FriendlyUnitInfo): Boolean = {
    if (unit.unitClass.melee) return shouldKiteMelee(unit)

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

  /**
   * Melee kite gate (thin seam over the pure kernel): blades yield
   * only on the kernel's terms - nearby lings, a superior zealot pack,
   * or a dying hull still swinging at melee. Anything else stands.
   */
  private def shouldKiteMelee(unit: FriendlyUnitInfo): Boolean = {
    val marginPixels = KiteAway.enemyMarginTiles * 32
    val foeZealots = unit.matchups.threatsInPixels(marginPixels).count(Protoss.Zealot)
    val mates = unit.matchups.allies.count(a =>
      a.alive && a.unitClass.isWarrior && a.pixelDistanceEdge(unit) <= marginPixels)

    MeleeKiteAway.shouldRetreat(
      totalHp = unit.totalHealth,
      coolingDown = unit.cooldownLeft > 0,
      hasLingNearby = unit.matchups.threatsInPixels(marginPixels).exists(Zerg.Zergling),
      outnumberedByZealots = foeZealots > mates + 1,
      attackingMelee = unit.matchups.targetNearest.exists(_.unitClass.melee))
  }

  // Hold-to-shoot: a committed chaser met with a ready gun is a free shot,
  // not a reason to yield ground. Fires only when we'd otherwise kite, and
  // never steals another duel's target: the chaser must be ours or none.
  // Vultures duel on their own rule: nearly-ready guns fire in range, and
  // head-on sprinters meet the shot at bonus reach - kiting a Vulture
  // donates the exact distance its speed wants.
  private def holdingShot(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    val chaser = Maff.minBy(unit.matchups.threats)(_.pixelDistanceEdge(unit))
    chaser.exists(c => combat.target.forall(_.id == c.id) && holdsAgainst(unit, c))
  }

  private def holdsAgainst(unit: FriendlyUnitInfo, chaser: UnitInfo): Boolean = {
    if (Terran.Vulture(chaser)) {
      HoldToShoot.shouldStandVsVulture(
        unit.pixelDistanceEdge(chaser),
        chaser.pixelRangeAgainst(unit),
        unit.cooldownLeft,
        With.latency.latencyFrames,
        chaser.speedApproaching(unit.pixel),
        FacingAway.facingUs(chaser.angleRadians, chaser.pixel.radiansTo(unit.pixel)))
    } else {
      HoldToShoot.shouldStand(
        unit.pixelDistanceEdge(chaser),
        HoldToShoot.effectiveRange(chaser.pixelRangeAgainst(unit), chaser.topSpeed, windupFrames = 0, unit.topSpeed * unit.unitClass.accelerationFrames / 2.0),
        unit.cooldownLeft,
        With.latency.latencyFrames,
        chaser.speedApproaching(unit.pixel),
        HoldToShoot.holdMarginFrames)
    }
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
    val offsetPixels = WoundMargin(0.0, KiteAway.maxOffsetTiles).pixels(unit.woundPercent())
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

    val threat = Maff.minBy(unit.matchups.threats.filter(_.unitClass.isStaticDefense))(_.pixelDistanceEdge(unit))
    if (threat.isEmpty) return false

    val gap = standoffGap(unit, threat.get)
    if (gap <= 0) return false

    stepOut(unit, combat, threat.get, gap)

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

  private def standoffGap(unit: FriendlyUnitInfo, threat: UnitInfo): Double =
    ClusterSafety.standoffGapPixels(
      unit.pixelDistanceEdge(threat),
      threat.pixelRangeAgainst(unit),
      KiteAway.enemyMarginTiles,
      clusterAttacked = true)

  private def stepOut(unit: FriendlyUnitInfo, combat: Combat, threat: UnitInfo, gap: Double): Unit = {
    unit.agent.act("StaticStandoff")
    unit.agent.decision.set(unit.pixel.project(threat.pixel, -gap).walkablePixel)
    combat.maneuvers.move()
  }
}
