package Micro.Actions.Protoss.Detector

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Protoss.Air.BeFlier
import Micro.Actions.Combat.Maneuvering.{RangedAvoidance, RangedReach}
import Micro.Actions.Combat.Tactics.{Recharge, Spot}
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
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
    // Survival first: a defenseless eye never trades with guns that can
    // hit it - outrun any enemy ranged unit on the wound-scaled margin.
    if (fleeAttackers(unit)) return

    // Spread outranks everything but survival: stacked eyes die together,
    // and every Spot/combat step after this one walks them back into the
    // same pile. While spreading the eye moves and reports, nothing else.
    if (spread(unit)) return

    if (keepOffDetectors(unit)) return

    Spot(unit)
    Recharge(unit)
    unit.agent.combat.perform(unit)
  }

  private def spread(unit: FriendlyUnitInfo): Boolean = {
    // Only older neighbors count: the newer eye moves, the older holds.
    // Engine edge distance is pixels; the threshold is tiles (rule 12).
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

  /**
   * Shared avoidance: any enemy ranged unit. Wound-scaled 6-to-12 tile
   * margin: unlike a Corsair the eye never shoots back, so even a fresh
   * hull holds clear of gun reach instead of skating inside it.
   */
  val avoidance = new RangedAvoidance(marginLowTiles = 6.0, marginHighTiles = 12.0)

  /** Flee guns: a defenseless eye outruns any enemy ranged unit that can hit it. */
  private def fleeAttackers(unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Observer(unit)) return false

    val threats = With.units.enemy.view.filter(e =>
      e.alive && e.likelyStillThere && e.unitClass.canAttack(unit)
        && RangedReach.isRanged(e.pixelRangeAgainst(unit))).map(_.pixel).toSeq
    avoidance.fleePixel(unit.pixel, threats, unit.woundPercent()).exists(dest => {
      unit.cherryVisUnitLog("ObserverFlee")
      unit.agent.destinationNext.set(dest)
      Commander.move(unit)
      true
    })
  }

  /** Observer keep-off: never close inside six tiles of an enemy detector; back straight out instead. */
  private def keepOffDetectors(unit: FriendlyUnitInfo): Boolean = {    if ( ! Protoss.Observer(unit)) return false

    // Nearest enemy detector, regardless of id: foe ids carry no seniority.
    val nearest = Maff.minBy(With.units.enemy.view.filter(e => e.alive && e.unitClass.isDetector))(unit.pixelDistanceEdge)
    nearest.exists(foe => {
      if (DetectorSpread.shouldYieldToDetector(unit.tileDistanceEdge(foe) * Maff.inv32)) {
        unit.cherryVisUnitLog("ObserverStandOff")
        unit.agent.destinationNext.set(DetectorSpread.standOff(unit.pixel, foe.pixel))
        Commander.move(unit)
        true
      } else false
    })
  }
}
