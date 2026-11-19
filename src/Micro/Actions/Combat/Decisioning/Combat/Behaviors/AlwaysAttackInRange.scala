package Micro.Actions.Combat.Decisioning

import Mathematics.Maff
import Micro.Actions.Combat.Decisioning.CrucialAirSnipe._
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Always-attack victims in reach (was CrucialAirSnipeBehavior): a grounded
 * warrior shoots the targets that must die first, regardless of
 * anything else the unit is doing - even retreating. Air victims need
 * anti-air guns, ground victims need anti-ground ones; air always
 * outranks ground and each half walks its victim down inside the chase
 * margin instead of watching it leave - only fast runners (Scourge,
 * Wraith showing its engines) are let go. Fixed priority: suicide
 * Scourge nearest, then the weakest Carrier, Science Vessel,
 * Battlecruiser, Wraith, detected Observer, Reaver, Dark Templar,
 * High Templar. Returns true when it shot or is already on the victim.
 */
object AlwaysAttackInRange {

  def run(unit: FriendlyUnitInfo): Boolean = {
    if (unit.flying) return false

    if ( ! unit.unitClass.isWarrior) return false

    // Air victims need anti-air guns, ground victims need anti-ground
    // ones - a split that returns early instead of scanning the wrong
    // half of the ladder. Air always outranks ground (the pick order
    // below), so the two halves only ever merge for the shot.
    val air: Seq[(UnitInfo, VictimKind)] =
      if (unit.unitClass.attacksAir) victimsOf(unit, flying = true) else Seq.empty
    val ground: Seq[(UnitInfo, VictimKind)] =
      if (unit.unitClass.attacksGround) victimsOf(unit, flying = false) else Seq.empty
    val victims = (air ++ ground)
      // Dark Templars duel on terms: a nearly-ready gun, and room to
      // stand for ranged hulls - planting feet on top of the swipe
      // trades the volley for DT hits. Melee answers whenever warm.
      .filterNot { case (t, _) => Protoss.DarkTemplar(t) && ! CrucialAirSnipe.duelsDarkTemplar(
        unit.cooldownLeft,
        unit.unitClass.melee,
        unit.pixelDistanceEdge(t) * Maff.inv32) }
      .map { case (t, _) => t }.toVector
    if (victims.isEmpty) return false

    val marks = victims.map(t => Mark(
      distTiles = unit.pixelDistanceEdge(t) * Maff.inv32,
      kind = kindOf(t).get,
      totalHealth = t.totalHealth))
    CrucialAirSnipe.pick(marks.toSeq).foreach(i =>
      NearestShot.take(unit, "AlwaysAttack", Seq(victims(i))))

    true
  }

  /** Victims of one half of the sky: the kind decides the ladder, the
    * margin decides the walk-in. The Atlantis reach check prices the
    * shot the same way for every victim - in range or at most a margin
    * past it - and the kernel vetoes only fast runners. */
  private def victimsOf(unit: FriendlyUnitInfo, flying: Boolean): Seq[(UnitInfo, VictimKind)] =
    unit.matchups.targets.view.flatMap(t =>
      kindOf(t).filter(_ => t.visible && t.flying == flying).map(kind =>
        (t, kind))).filter { case (t, kind) =>
      CrucialAirSnipe.inSnipeReach(
        kind,
        unit.inRangeToAttack(t),
        unit.canAttackWithMargin(CrucialAirSnipe.chaseMarginTiles, t),
        unit.isOtherUnitFacingUs(t))
    }.toSeq

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
