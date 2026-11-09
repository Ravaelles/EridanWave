package Micro.Corsair

import Mathematics.Points.Pixel
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Tactic.Squads.SquadAcePilots

/**
 * Shared order-issuing for Corsair behaviors: one shape for claiming
 * a unit (intents + act label) instead of a copy per behavior.
 */
object CorsairAct {

  def order(squad: SquadAcePilots, unit: FriendlyUnitInfo, name: String, target: UnitInfo, terminus: Pixel): Boolean = {
    unit.intend(squad).setTargets(Seq(target)).setTerminus(terminus)
    unit.agent.act(name)
    true
  }

  def move(squad: SquadAcePilots, unit: FriendlyUnitInfo, name: String, terminus: Pixel): Boolean = {
    unit.intend(squad).setTerminus(terminus)
    unit.agent.act(name)
    true
  }
}
