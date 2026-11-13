package Micro.Actions.Protoss.Detector

import Lifecycle.With
import Micro.Actions.Action
import Micro.Actions.Protoss.Air.BeFlier
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
    // Spread outranks everything but survival: stacked eyes die together,
    // and every Spot/combat step after this one walks them back into the
    // same pile. While spreading the eye moves and reports, nothing else.
    if (spread(unit)) return

    Spot(unit)
    Recharge(unit)
    unit.agent.combat.perform(unit)
  }

  private def spread(unit: FriendlyUnitInfo): Boolean = {
    // Only older neighbors count: the newer eye moves, the older holds.
    // Engine edge distance is pixels; the threshold is tiles (rule 10).
    val others = With.units.ours.view
      .filter(u => u.unitClass == unit.unitClass && u.id != unit.id)
      .map(u => DetectorSpread.OlderEye(u.id, u.pixel, u.tileDistanceEdge(unit)))
      .toSeq
    DetectorSpread.partner(unit.id, others).exists(older => {
      if (DetectorSpread.shouldSpread(unit.id, older.id, older.distTiles)) {
        unit.cherryVisUnitLog("DetectorSpread")
        unit.agent.destinationNext.set(DetectorSpread.awayFrom(unit.pixel, older.pixel))
        Commander.move(unit)
        true
      } else false
    })
  }
}
