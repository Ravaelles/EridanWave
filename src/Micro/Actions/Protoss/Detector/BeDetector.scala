package Micro.Actions.Protoss.Detector

import Lifecycle.With
import Mathematics.{Maff, Separation}
import Micro.Actions.Action
import Micro.Actions.Protoss.Air.BeFlier
import Micro.Actions.Combat.Maneuvering.AirFlee
import Micro.Actions.Combat.Maneuvering.CornerAntiStuckGuard
import Micro.Actions.Combat.Tactics.{Recharge, Spot}
import Micro.Agency.Commander
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
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
  override def isContainer: Boolean = true

  override def allowed(unit: FriendlyUnitInfo): Boolean =
    unit.unitClass.isDetector && unit.canMove

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    // Genuine threats first: a flee order sticks in the engine while a
    // later one is suppressed, so the widest flee must claim before the
    // narrower steering below overwrites its destination.
    if (fleeAttackers(unit)) return
    // Compromised eyes run from fast hunters early: with a detector
    // inside eleven tiles the eye is seen, and Scourge, Corsairs,
    // Wraiths and Mutalisks close too fast to outrun once committed.
    if (fleeFastHunters(unit)) return

    BeFlier(unit)
    // Survival owns the frame: once anything above moved, later claims
    // must not overwrite its destination with a suppressed order.
    if (unit.unready) return

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
      .map(u => Separation.Neighbor(u.id, u.pixel, u.tileDistanceEdge(unit)))
      .toSeq
    Separation.partner(unit.id, others).exists(older => {
      if (Separation.shouldSpread(unit.id, older.id, older.distTiles, DetectorSpread.minDistTiles)) {
        unit.cherryVisUnitLog("DetectorSpread")
        unit.agent.forceAway(unit.pixel, older.pixel, DetectorSpread.minDistTiles)
        Commander.move(unit)
        true
      } else false
    })
  }

  /**
   * Eyes keep a wider berth than shooters: unlike a Corsair the eye
   * never shoots back, so even a fresh hull holds clear of gun reach
   * instead of skating inside it. Same shared rule, wider band.
   */
  val marginLowTiles: Double = 6.0
  val marginHighTiles: Double = 12.0

  /**
   * Fast-hunter early warning: a detector this close, in tiles, means the
   * eye is compromised, so Scourge, Corsairs, Wraiths and Mutalisks -
   * too fast to outrun once committed - trigger the flee from twice as
   * far out as ordinary guns. Pure numbers for the suite.
   */
  val hunterFleeDetectorTiles: Double = 11.0
  val hunterFleeThreatTiles: Double = 20.0

  def shouldFleeHunters(detectorTiles: Double, hunterTiles: Double): Boolean =
    detectorTiles <= hunterFleeDetectorTiles && hunterTiles <= hunterFleeThreatTiles

  /** Flee fast hunters on early warning: detector inside eleven tiles means seen. */
  private def fleeFastHunters(unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Observer(unit)) return false

    val detectorTiles = Maff.minBy(With.units.enemy.view.filter(e =>
      e.alive && e.unitClass.isDetector))(_.tileDistanceCenter(unit.pixel))
      .map(e => unit.tileDistanceCenter(e.pixel))
      .getOrElse(Double.PositiveInfinity)
    val hunterPixel = Maff.minBy(With.units.enemy.view.filter(e =>
      e.alive && e.likelyStillThere && e.isAny(Zerg.Scourge, Protoss.Corsair, Terran.Wraith, Zerg.Mutalisk)))(
      _.pixelDistanceCenter(unit.pixel))
    (for (hunter <- hunterPixel; if shouldFleeHunters(detectorTiles, unit.tileDistanceCenter(hunter.pixel))) yield {
      unit.cherryVisUnitLog("ObserverFleeHunters")
      unit.agent.forceTo(CornerAntiStuckGuard.guard(
        AirFlee.fleeFrom(unit.pixel, hunter.pixel, AirFlee.marginTiles(unit.woundPercent(), marginLowTiles, marginHighTiles)),
        With.mapPixelWidth,
        With.mapPixelHeight))
      Commander.move(unit)
      true
    }).getOrElse(false)
  }

  /** Flee guns: a defenseless eye outruns any enemy unit that can hit it. */
  private def fleeAttackers(unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Observer(unit)) return false

    val threats = With.units.enemy.view.filter(e =>
      e.alive && e.likelyStillThere && e.unitClass.canAttack(unit)).map(_.pixel).toSeq
    AirFlee.fleeScored(
      unit.pixel,
      threats.map(t => (t, AirFlee.triggerTiles * 32.0 - unit.pixelDistanceCenter(t))),
      _ > 0.0,
      AirFlee.marginTiles(unit.woundPercent(), marginLowTiles, marginHighTiles) * 32.0,
      With.mapPixelWidth,
      With.mapPixelHeight).exists(dest => {
      unit.cherryVisUnitLog("ObserverFlee")
      unit.agent.forceTo(dest)
      Commander.move(unit)
      true
    })
  }

  /** Observer keep-off: never close inside six tiles of an enemy detector; back straight out instead. */
  private def keepOffDetectors(unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Observer(unit)) return false

    // Nearest enemy detector, regardless of id: foe ids carry no seniority.
    val nearest = Maff.minBy(With.units.enemy.view.filter(e => e.alive && e.unitClass.isDetector))(unit.pixelDistanceEdge)
    // Latch-input probe: the standoff flips run-length-one in hunter
    // games and the latch itself is pinned, so either the detector set
    // or visibility flickers underneath it. Log the inputs per frame -
    // detector id (or none), distance, visibility, latch verdict - and
    // read the flicker off the log instead of guessing.
    nearest match {
      case Some(foe) =>
        val distTiles = unit.tileDistanceEdge(foe)
        val latched = DetectorSpread.standOffLatched(unit.id, distTiles)
        Debugging.Probe.note("StandOff", unit.id, f"det=${foe.id} dist=$distTiles%.1f vis=${foe.visible} latched=$latched")
        if (latched) {
          unit.cherryVisUnitLog("ObserverStandOff")
          unit.agent.forceTo(DetectorSpread.standOff(unit.pixel, foe.pixel))
          Commander.move(unit)
          true
        } else false
      case None =>
        Debugging.Probe.note("StandOff", unit.id, "det=none")
        false
    }
  }
}
