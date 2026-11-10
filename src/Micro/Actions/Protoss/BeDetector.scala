package Micro.Actions.Protoss

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Combat.Tactics.{Recharge, Spot}
import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Detector driver: eyes never belonged in Fight - they cannot shoot, so a
 * combat chain dispatching on them is a category error. This runs instead:
 * Scourge-dodge first, then the Spot and Recharge duties Fight used to
 * cover, then the nine-tile spread, then intent execution. Same duties as
 * before, honest dispatch. Monitor eyes (isScout) ride along: they never
 * entered Fight either, and stacked watches die together.
 */
object BeDetector extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean =
    unit.unitClass.isDetector && unit.canMove

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    BeFlier(unit)
    Spot(unit)
    Recharge(unit)
    spread(unit)
    unit.agent.combat.perform(unit)
  }

  private def spread(unit: FriendlyUnitInfo): Unit = {
    // Only older neighbors count: the newer eye moves, the older holds.
    // Engine edge distance is pixels; the threshold is tiles (rule 10).
    Maff.minBy(With.units.ours.view.filter(u => u.unitClass == unit.unitClass && u.id < unit.id))(_.pixelDistanceEdge(unit))
      .foreach(older => {
        if (DetectorSpread.shouldSpread(unit.id, older.id, older.pixelDistanceEdge(unit) / 32.0)) {
          unit.agent.destinationNext.set(DetectorSpread.awayFrom(unit.pixel, older.pixel))
          Commander.move(unit)
        }
      })
  }
}
