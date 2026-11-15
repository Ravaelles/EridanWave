package Micro.Actions.Combat.Decisioning

import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.CrucialAirSnipe._
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Crucial air-victim snipe: a grounded anti-air warrior shoots Scourge,
 * Carriers, Vessels, Battlecruisers and Wraiths on sight, in that order -
 * ahead of kiting, dressing, rallying and even retreating. An incoming
 * Scourge gets sniped from 1.5 tiles past the range edge, since waiting
 * for contact means eating it. A Scourge that
 * lives one frame too long kills a Shuttle; a Carrier left alone kills
 * the army. Returns true when it shot or is already on the victim.
 */
object CrucialAirSnipeBehavior {

  def run(unit: FriendlyUnitInfo): Boolean = {
    if (unit.flying) return false

    if ( ! unit.unitClass.isWarrior) return false

    if ( ! unit.unitClass.attacksAir) return false

    val victims = unit.matchups.targets.view.filter(t =>
      t.visible && kindOf(t).isDefined && CrucialAirSnipe.inSnipeReach(
        unit.inRangeToAttack(t),
        Zerg.Scourge(t),
        unit.isOtherUnitFacingUs(t),
        unit.pixelsToGetInRange(t) * Maff.inv32)).toVector
    if (victims.isEmpty) return false

    val marks = victims.map(t => Mark(
      distTiles = unit.pixelDistanceEdge(t) * Maff.inv32,
      kind = kindOf(t).get,
      totalHealth = t.totalHealth))
    CrucialAirSnipe.pick(marks.toSeq).foreach(i =>
      NearestShot.take(unit, "CrucialSnipe", Seq(victims(i))))

    true
  }

  private def kindOf(target: UnitInfo): Option[VictimKind] = {
    if (Zerg.Scourge(target)) Some(Scourge)
    else if (Protoss.Carrier(target)) Some(Carrier)
    else if (Terran.ScienceVessel(target)) Some(Vessel)
    else if (Terran.Battlecruiser(target)) Some(Battlecruiser)
    else if (Terran.Wraith(target)) Some(Wraith)
    else None
  }
}
