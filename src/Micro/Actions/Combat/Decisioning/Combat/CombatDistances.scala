package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
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

    val t = combat.target.get
    lazy val distance           = unit.pixelDistanceEdge(t)
    lazy val range              = unit.pixelRangeAgainst(t)
    lazy val rangeAgainstUs     = SomeIf(t.canAttack(unit), t.pixelRangeAgainst(unit))
    lazy val rangeEqual         = rangeAgainstUs.contains(range)
    lazy val pixelsOutranged    = rangeAgainstUs.map(_ - unit.pixelRangeAgainst(t)).filter(_ > 0)
    lazy val scourgeApproaching = unit.matchups.threats.exists(t => Zerg.Scourge(t) && t.pixelDistanceEdge(unit) < 32 * 5)
    lazy val projectedUs        = unit.pixel.projectUpTo(t.pixel, 16)
    lazy val projectedTarget    = t.pixel.projectUpTo(t.presumptiveDestinationNext, 16)
    lazy val targetApproaching  = unit.pixelDistanceSquared(t) > unit.pixelDistanceSquared(projectedTarget)
    lazy val targetEscaping     = unit.pixelDistanceSquared(t) < unit.pixelDistanceSquared(projectedTarget)
    lazy val inChoke            = ! unit.flying && unit.zone.edges.exists(_.contains(unit.pixel))
    lazy val sameThreatsChasing = unit.matchups.threats.forall(t => t.inRangeToAttack(unit) == t.inRangeToAttack(unit, projectedUs))
    lazy val obiwanned          = ! unit.flying && ! t.flying && ! unit.unitClass.melee && t.altitude > unit.altitude
    lazy val chaseDistance      = rangeAgainstUs.filter(_ < range).map(r => Math.max(24, (range - r) / 2)).getOrElse(0d)
    // Push through choke
    if ( ! unit.flying && ! t.canMove && ! t.canAttack(unit)) return ?(inChoke && combat.nudgedTowards, 0, range)
    // Some units shouldn't even try to chase
    if (unit.isAny(Terran.SiegeTankUnsieged, Protoss.Reaver)) return range
    // If we're safe, why not chase
    if (unit.matchups.threatDeepest.isEmpty) return 0
    // Some units should usually chase flying targets
    if (unit.isAny(Terran.Wraith, Protoss.Corsair, Protoss.Scout, Zerg.Mutalisk) && t.flying && ! scourgeApproaching && ( ! Zerg.Overlord(t) || unit.matchups.pixelsEntangled < -64)) return 0
    // Chase if they outrange us
    if (pixelsOutranged.isDefined) return chaseDistance
    // Chase if we're in a choke and being nudged towards them
    if (inChoke && combat.nudgedTowards) return chaseDistance
    // Chase if we're confident and fighting up high ground
    if (obiwanned) return chaseDistance
    // Chase if we're confident and being nudged towards them
    if (combat.confidentEnoughToChase && combat.nudgedTowards) return chaseDistance
    // Chase if we're confident and they're about to escape us
    if (combat.confidentEnoughToChase && targetEscaping) return chaseDistance
    // Chase if they're escaping out of our ideal range and chasing won't bring us into range of anyone new
    if (targetEscaping && sameThreatsChasing) return chaseDistance
    // Breathe if range is equal and they shoot first
    if (rangeEqual && t.cooldownLeft < unit.cooldownLeft) return range + unit.topSpeed * Math.max(0, unit.cooldownLeft - With.latency.latencyFrames - unit.framesToTurnTo(t))
    // Maintain maximum range if chasing puts us in range of anyone new
    if ( ! sameThreatsChasing) return range
    // Maintain maximum range if they're a threat coming towards us
    if (targetApproaching) return range
    // Maintain just enough range if we're being nudged
    if (combat.nudgedTowards) return chaseDistance
    // Otherwise, stay near max range. Get a little closer if we can in case they try to flee
    rangeAgainstUs.map(rangeAgainst => Maff.clamp(rangeAgainst + 64, range - 16, range)).getOrElse(range - 16)
  }
}
