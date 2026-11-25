package Micro.Actions.Combat.Spells.Terran

import Micro.Actions.Combat.Spells.TargetedSpell
import ProxyBwapi.Races.Terran
import ProxyBwapi.Techs.Tech
import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitInfo._

object Yamato extends TargetedSpell {

  /** Yamato gun: one high-value victim at Battlecruiser range, priced by reach and entanglement. */
  override protected def casterClass    : UnitClass = Terran.Battlecruiser
  override protected def tech           : Tech      = Terran.Yamato
  override protected def aoe            : Boolean   = false
  override protected def castRangeTiles : Int       = 10
  override protected def thresholdValue : Double    = Terran.Goliath.subjectiveValue

  override protected def valueTarget(target: UnitInfo, caster: FriendlyUnitInfo): Double = {
    if ( ! target.isEnemy) return 0.0

    TargetedSpell.reachValue(target.subjectiveValue, caster.pixelDistanceEdge(target) - 32 * castRangeTiles, caster.matchups.pixelsEntangled)
  }
}
