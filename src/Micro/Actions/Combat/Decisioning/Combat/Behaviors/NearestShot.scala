package Micro.Actions.Combat.Decisioning

import Mathematics.Maff
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Nearest-shot helper (shared tail of the engage behaviours): issue the
 * attack order at the nearest candidate unless already on it. One home so
 * the "shoot the nearest hittable" shape cannot drift apart again: a fifth
 * behaviour that shoots the nearest thing costs one line. Callers pass a
 * non-empty list (their gates already checked); an empty list shoots
 * nothing and reports false instead of crashing on the pick.
 */
object NearestShot {

  def take(unit: FriendlyUnitInfo, label: String, candidates: Iterable[UnitInfo]): Boolean =
    take(unit, label, candidates, unit.pixelDistanceEdge(_))

  def take(unit: FriendlyUnitInfo, label: String, candidates: Iterable[UnitInfo], rankBy: UnitInfo => Double): Boolean = {
    Maff.minBy(candidates)(rankBy).map(target => {
      if ( ! unit.orderTarget.contains(target)) {
        unit.agent.act(label)
        Commander.attack(unit, target)
      }

      true
    }).getOrElse(false)
  }
}
