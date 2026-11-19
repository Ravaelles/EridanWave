package Micro.Targeting

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Wall-breach targeting (was Combat.acquireTarget's inline block): no nav
 * path to the goal means the wall is the fight - smash the nearest
 * blocking building instead of bouncing off it. Fires only for grounded
 * warriors at a live goal, and yields to a visible reachable duel - a
 * flanker behind us still outranks masonry.
 */
object WallBreachTarget {

  def run(unit: FriendlyUnitInfo): Unit = {
    // Dark Templars own their targets: the breach never claims the blade.
    if (Protoss.DarkTemplar(unit)) return
    // Outgunned line holds off the wall for the rest of the game.
    if (WallInBan.banned()) return

    if (unit.unitClass.isWarrior && ! unit.flying && ! unit.loaded && unit.tile.valid) {
      val goalBlocked = ! With.paths.groundPathExists(unit.tile, unit.agent.destinationNext().tile)
      val dueling = unit.agent.toAttack.exists(t =>
        t.visible && t.tile.valid && With.paths.groundPathExists(unit.tile, t.tile))
      if (goalBlocked && ! dueling) {
        Maff.minBy(With.units.enemy.view.filter(e =>
          e.unitClass.isBuilding
          && e.likelyStillThere
          && BreakTerranWallIn.wallPieceLegal(Terran.Barracks(e), TerranWallIn.blocked)
          && WallBreach.breaches(noGroundPath = true, unit.pixelDistanceEdge(e) * Maff.inv32)))(
          unit.pixelDistanceEdge(_)).foreach(b => unit.agent.toAttack = Some(b))
      }
    }
  }
}
