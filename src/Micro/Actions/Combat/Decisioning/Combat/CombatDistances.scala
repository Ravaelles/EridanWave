package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.Kiting.ApproachHelpless
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.?
import Utilities.SomeIf

class CombatDistances(combat: Combat, unit: FriendlyUnitInfo) {

  private[Decisioning] def readyToApproachTarget: Boolean = {
    if (combat.target.isEmpty) return false
    if (unit.unitClass.melee) return true
    if (?(unit.airlifted, 0, unit.framesToBeReadyForAttackOrder(combat.target.get)) > unit.framesToGetInRange(combat.target.get) + 4) return false

    true
  }

  private[Decisioning] def getIdealTargetDistance: Double = {
    if (combat.target.isEmpty) return 0.0

    val readings = new TargetReadings(combat.target.get)
    noChaseCases(readings)
      .orElse(chaseCases(readings))
      .orElse(holdCases(readings))
      .getOrElse(defaultCase(readings))
  }

  // Otherwise, stay near max range. Get a little closer if we can in case they try to flee.
  private def defaultCase(g: TargetReadings): Double =
    g.rangeAgainstUs.map(rangeAgainst => Maff.clamp(rangeAgainst + 64, g.range - 16, g.range)).getOrElse(g.range - 16)

  // Per-target geometry the distance rules read: one named fact per line,
  // shared by every rule so no branch recomputes the same sight picture.
  private class TargetReadings(val target: UnitInfo) {
    lazy val distance           = unit.pixelDistanceEdge(target)
    lazy val range              = unit.pixelRangeAgainst(target)
    lazy val rangeAgainstUs     = SomeIf(target.canAttack(unit), target.pixelRangeAgainst(unit))
    lazy val rangeEqual         = rangeAgainstUs.contains(range)
    lazy val pixelsOutranged    = rangeAgainstUs.map(_ - unit.pixelRangeAgainst(target)).filter(_ > 0)
    lazy val scourgeApproaching = unit.matchups.threats.exists(t => Zerg.Scourge(t) && t.pixelDistanceEdge(unit) < 32 * 5)
    lazy val projectedUs        = unit.pixel.projectUpTo(target.pixel, 16)
    lazy val projectedTarget    = target.pixel.projectUpTo(target.presumptiveDestinationNext, 16)
    lazy val targetApproaching  = unit.pixelDistanceSquared(target) > unit.pixelDistanceSquared(projectedTarget)
    lazy val targetEscaping     = unit.pixelDistanceSquared(target) < unit.pixelDistanceSquared(projectedTarget)
    lazy val inChoke            = ! unit.flying && unit.zone.edges.exists(_.contains(unit.pixel))
    lazy val sameThreatsChasing = unit.matchups.threats.forall(t => t.inRangeToAttack(unit) == t.inRangeToAttack(unit, projectedUs))
    lazy val obiwanned          = ! unit.flying && ! target.flying && ! unit.unitClass.melee && target.altitude > unit.altitude
    lazy val chaseDistance      = rangeAgainstUs.filter(_ < range).map(r => Math.max(24, (range - r) / 2)).getOrElse(0d)
  }

  private def noChaseCases(g: TargetReadings): Option[Double] = {
    // Push through choke
    if ( ! unit.flying && ! g.target.canMove && ! g.target.canAttack(unit)) Some(?(g.inChoke && combat.nudgedTowards, 0, g.range))
    // Some units shouldn't even try to chase
    else if (unit.isAny(Terran.SiegeTankUnsieged, Protoss.Reaver)) Some(g.range)
    // If we're safe, why not chase
    else if (unit.matchups.threatDeepest.isEmpty) Some(0)
    // Some units should usually chase flying targets
    else if (unit.isAny(Terran.Wraith, Protoss.Corsair, Protoss.Scout, Zerg.Mutalisk) && g.target.flying && ! g.scourgeApproaching && ( ! Zerg.Overlord(g.target) || unit.matchups.pixelsEntangled < -64)) Some(0)
    // Close what cannot hit back: harmless air always, buildings at full
    // health. Wounded hulls keep their range against buildings instead.
    else if (ApproachHelpless.shouldClose(g.rangeAgainstUs.isEmpty, g.target.flying, unit.woundPercent() <= 0.0, unit.pixel.groundPixels(g.target.pixel) * Maff.inv32)) Some(0)
    else None
  }

  private def chaseCases(g: TargetReadings): Option[Double] = {
    // Chase if they outrange us
    if (g.pixelsOutranged.isDefined) Some(g.chaseDistance)
    // Chase if we're in a choke and being nudged towards them
    else if (g.inChoke && combat.nudgedTowards) Some(g.chaseDistance)
    // Chase if we're confident and fighting up high ground
    else if (g.obiwanned) Some(g.chaseDistance)
    // Chase if we're confident and being nudged towards them
    else if (combat.confidentEnoughToChase && combat.nudgedTowards) Some(g.chaseDistance)
    // Chase if we're confident and they're about to escape us
    else if (combat.confidentEnoughToChase && g.targetEscaping) Some(g.chaseDistance)
    // Chase if they're escaping out of our ideal range and chasing won't bring us into range of anyone new
    else if (g.targetEscaping && g.sameThreatsChasing) Some(g.chaseDistance)
    else None
  }

  private def holdCases(g: TargetReadings): Option[Double] = {
    // Breathe if range is equal and they shoot first
    if (g.rangeEqual && g.target.cooldownLeft < unit.cooldownLeft) Some(g.range + unit.topSpeed * Math.max(0, unit.cooldownLeft - With.latency.latencyFrames - unit.framesToTurnTo(g.target)))
    // Maintain maximum range if chasing puts us in range of anyone new
    else if ( ! g.sameThreatsChasing) Some(g.range)
    // Maintain maximum range if they're a threat coming towards us
    else if (g.targetApproaching) Some(g.range)
    // Maintain just enough range if we're being nudged
    else if (combat.nudgedTowards) Some(g.chaseDistance)
    else None
  }
}
