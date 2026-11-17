package Micro.Actions.Combat.Decisioning

import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.CrucialAirSnipe._
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Always-attack victims in range (was CrucialAirSnipeBehavior): a grounded
 * anti-air warrior shoots the targets that must die first, regardless of
 * anything else the unit is doing - even retreating. Fixed priority:
 * suicide Scourge nearest, then the weakest Carrier, Science Vessel,
 * Battlecruiser, Wraith, detected Observer, Reaver, Dark Templar,
 * High Templar. Returns true when it shot or is already on the victim.
 */
object AlwaysAttackInRange {

  def run(unit: FriendlyUnitInfo): Boolean = {
    if (unit.flying) return false

    if ( ! unit.unitClass.isWarrior) return false

    if ( ! unit.unitClass.attacksAir) return false

    val victims = unit.matchups.targets.view.filter(t =>
      t.visible && kindOf(t).isDefined && CrucialAirSnipe.inSnipeReach(
        unit.inRangeToAttack(t),
        Zerg.Scourge(t),
        unit.isOtherUnitFacingUs(t),
        unit.pixelsToGetInRange(t) * Maff.inv32))
      // Dark Templars duel on terms: a nearly-ready gun, and room to
      // stand for ranged hulls - planting feet on top of the swipe
      // trades the volley for DT hits. Melee answers whenever warm.
      .filterNot(t => Protoss.DarkTemplar(t) && ! CrucialAirSnipe.duelsDarkTemplar(
        unit.cooldownLeft,
        unit.unitClass.melee,
        unit.pixelDistanceEdge(t) * Maff.inv32)).toVector
    if (victims.isEmpty) return false

    val marks = victims.map(t => Mark(
      distTiles = unit.pixelDistanceEdge(t) * Maff.inv32,
      kind = kindOf(t).get,
      totalHealth = t.totalHealth))
    CrucialAirSnipe.pick(marks.toSeq).foreach(i =>
      NearestShot.take(unit, "AlwaysAttack", Seq(victims(i))))

    true
  }

  private def kindOf(target: UnitInfo): Option[VictimKind] = {
    if (Zerg.Scourge(target)) Some(Scourge)
    else if (Protoss.Carrier(target)) Some(Carrier)
    else if (Terran.ScienceVessel(target)) Some(Vessel)
    else if (Terran.Battlecruiser(target)) Some(Battlecruiser)
    else if (Terran.Wraith(target)) Some(Wraith)
    else if (Protoss.Observer(target) && ! target.effectivelyCloaked) Some(Observer)
    else if (Protoss.Reaver(target)) Some(Reaver)
    else if (Protoss.DarkTemplar(target)) Some(DarkTemplar)
    else if (Protoss.HighTemplar(target)) Some(HighTemplar)
    else None
  }
}
