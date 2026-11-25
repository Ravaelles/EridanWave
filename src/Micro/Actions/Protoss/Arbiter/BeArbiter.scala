package Micro.Actions.Protoss.Arbiter

import Micro.Actions.UnitControl
import Debugging.Visualizations.Forces
import Lifecycle.With
import Mathematics.{Maff, Separation, WoundMargin}
import Micro.Actions.Action
import Micro.Actions.Combat.Maneuvering.{AvoidDangerousUnits, Retreat}
import Micro.Actions.Combat.Tactics.Potshots.Potshot
import Micro.Actions.Protoss.Detector.DetectorSpread
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import Micro.Heuristics.Potential
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

object BeArbiter extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def appliesToUnit(unit: FriendlyUnitInfo): Boolean = Protoss.Arbiter(unit)

  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.Arbiter(unit)
  
  protected def needsUmbrella(target: UnitInfo): Boolean = target.isNone(
    Protoss.Arbiter,
    Protoss.DarkTemplar,
    Protoss.Interceptor,
    Protoss.Observer)

  val cloakRadiusPixels: Int = 64

  /**
   * Flee margin, in tiles: bold fresh hulls hold until six tiles, battered
   * ones break off up to eleven out - Corsair-style wound scaling for a
   * hull that must never trade. Applies on top of the entanglement check,
   * which still catches long-range threats the margin cannot see.
   */
  def fleeMarginTiles(wound01: Double): Double =
    2.0 + WoundMargin(4.0, 9.0).tiles(wound01)

  /**
   * Separation, exactly the Observer rule: same-type Arbiters keep nine
   * tiles apart and only the newer one moves, sliding out along the ray
   * to full separation. Claims the frame when it moves.
   */
  private def spread(arbiter: FriendlyUnitInfo): Boolean = {
    val others = With.units.ours.view
      .filter(u => Protoss.Arbiter(u) && u.id != arbiter.id)
      .map(u => Separation.Neighbor(u.id, u.pixel, u.tileDistanceEdge(arbiter)))
      .toSeq
    Separation.partner(arbiter.id, others).exists(older => {
      if (Separation.shouldSpread(arbiter.id, older.id, older.distTiles, DetectorSpread.minDistTiles)) {
        arbiter.cherryVisUnitLog("ArbiterSpread")
        arbiter.agent.forceAway(arbiter.pixel, older.pixel, DetectorSpread.minDistTiles)
        Commander.move(arbiter)
        true
      } else false
    })
  }

  override protected def perform(arbiter: FriendlyUnitInfo): UnitControl = {
    // Lockdown is instant death for a mechanical hull: a charged Ghost
    // turns the cloak before entanglement and spread - a frozen Arbiter
    // cloaks nothing, so the hull never stands inside lockdown reach.
    if (AvoidDangerousUnits.act(arbiter)) return UnitControl.claimedIf(! arbiter.ready)

    if (arbiter.matchups.pixelsEntangled > -16
      || arbiter.matchups.threats.exists(t =>
        arbiter.pixelDistanceEdge(t) < fleeMarginTiles(arbiter.woundPercent()) * 32.0)) {
      Retreat.delegate(arbiter)

      return UnitControl.claimedIf(! arbiter.ready)
    }

    val goal                              = arbiter.team.map(_.vanguardAll()).getOrElse(arbiter.agent.destinationNext())
    val goalDistanceSquared               = arbiter.pixelDistanceSquared(goal)
    if (spread(arbiter)) return UnitControl.claimedIf(! arbiter.ready)

    val seniorArbiters                    = arbiter.alliesSquad.filter(u => Protoss.Arbiter(u) && u.squadAge > arbiter.squadAge)
    val umbrellables                      = arbiter.alliesSquad.filter(needsUmbrella)
    val umbrellablesUncovered             = umbrellables.filterNot(u => seniorArbiters.exists(_.pixelDistanceCenter(u) < cloakRadiusPixels))
    val umbrellable                       = Maff.minBy(umbrellablesUncovered)(_.pixelDistanceSquared(goal))
    arbiter.agent.station.set(umbrellable.map(_.pixel).getOrElse(arbiter.agent.destinationNext()))
    arbiter.agent.forces(Forces.travel)   = Potential.towardsDestination(arbiter)
    arbiter.agent.forces(Forces.spacing)  = Potential.hardAvoidThreatRange(arbiter)
    MicroPathing.setWaypointForcefully(arbiter)
    if (arbiter.pixelDistanceCenter(arbiter.agent.destinationNext()) < 48 && arbiter.matchups.pixelsEntangled < 0) {
      Potshot.delegate(arbiter)
    }
    Commander.move(arbiter)
    UnitControl.claimedIf(! arbiter.ready)
  }
}


