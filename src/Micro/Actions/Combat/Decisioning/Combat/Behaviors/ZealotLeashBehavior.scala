package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.Maff
import Micro.Agency.Commander
import Micro.Actions.Protoss.Zealot.ZealotLeash
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Decisions.SuicideMission
import Utilities.Latch

/**
 * Zealot leash (one question: is this blade past its dragoons?): a Zealot
 * never operates past 5.5 tiles from our Dragoons - a lone charge into
 * their guns while ours fall back is a donation, not pressure. Only
 * blades; scouts, committed rushers, suicides and yolo pushes keep
 * their explicit orders, and with no Dragoons mustered there is nothing
 * to leash to.
 */
object ZealotLeashBehavior {

  /**
   * Leash latch (auto-registered, reset per game): the slot leg and the
   * leash leg share one actuator, so a raw 5.5-tile boundary trades the
   * Zealot every few frames instead of committing to either leg.
   */
  private val zealotLeash = new Latch

  def run(unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Zealot(unit)) return false
    if (unit.agent.isScout || unit.agent.commit || With.yolo.active || SuicideMission.held(unit)) return false

    val anchor = Maff.minBy(With.units.ours.view.filter(u => u != unit && u.alive && Protoss.Dragoon(u)))(
      _.pixelDistanceCenter(unit.pixel))
    if (anchor.isEmpty) return false

    val distTiles = anchor.get.pixelDistanceCenter(unit.pixel) * Maff.inv32
    // A blade on a sieged tank stays on it: the leash would trade a
    // free point-blank kill for a walk home, the same no-reason retreat
    // the technique floor already forbids.
    if (Maff.minBy(unit.matchups.enemies.view.filter(e =>
      e.alive && Terran.SiegeTankSieged(e)))(
      _.pixelDistanceEdge(unit)).exists(t => TankHug.holdsTank(unit.pixelDistanceEdge(t) * Maff.inv32, isMelee = true))) return false
    if ( ! zealotLeash(
      unit.id,
      fires = ZealotLeash.shouldRejoin(distTiles),
      releases = distTiles < ZealotLeash.leashReleaseTiles)) return false

    unit.agent.act("ZealotLeash")
    unit.agent.forceTo(anchor.get.pixel)
    Commander.move(unit)

    true
  }
}
