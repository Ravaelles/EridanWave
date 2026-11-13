package Micro.Actions.Protoss.ReaverShuttle

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Reaver half of the pair doctrine (first claim wins, then generic combat
 * duels): unload onto Zerg statics to kill them on the ground, hold and
 * shoot what the scarab can reach, board only under direct threat or for
 * a real trip - never circle. Anything else (kiting, chasing, marching)
 * stays with the generic fighter, which already knows how.
 */
object BeReaver extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.Reaver(unit)

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    if (unit.loaded) {
      if (shouldLand(unit)) {
        Commander.requestUnload(unit)
        unit.agent.act("UnloadStatic")
      }
      return
    }

    val foe = Maff.minBy(unit.matchups.targets.view.filter(t => ! t.flying && unit.canAttack(t)))(_.pixelDistanceEdge(unit))
    if (foe.exists(unit.inRangeToAttack)) {
      unit.agent.act("StandShoot")
      Commander.hold(unit)

      return
    }
    if (foe.exists(t => unit.tileDistanceEdge(t) < ReaverShuttleStaticAttack.engageTiles && ! outranges(t, unit))) {
      foe.foreach(t => {
        unit.agent.act("CloseStatic")
        unit.agent.destinationNext.set(t.pixel)
        Commander.move(unit)
      })

      return
    }
    val rideNearby = unit.agent.ride.exists(r => r.pixelDistanceEdge(unit) <= 64.0)
    if (rideNearby && (ReaverShuttleReboard.mayReboard(threatDist(unit), unit.woundPercent()) || needsRide(unit))) {
      unit.agent.act("BoardRide")
      Commander.rightClick(unit, unit.agent.ride.get)

      return
    }
    if (rideNearby && shotlessPressured(unit)) {
      unit.agent.act("BoardShotless")
      Commander.rightClick(unit, unit.agent.ride.get)
    }
  }

  /** Land on Zerg defense to kill it: close, undamaged on the way in, nothing inside the wound-scaled margin. */
  private def shouldLand(unit: FriendlyUnitInfo): Boolean = {
    val statics = unit.matchups.enemies.filter(e =>
      e.unitClass.isStaticDefense && e.player.isZerg && e.likelyStillThere && unit.tileDistanceEdge(e) < ReaverShuttleStaticAttack.engageTiles)
    statics.nonEmpty &&
      With.framesSince(unit.lastFrameTakingDamage) > 72 &&
      Maff.minBy(unit.matchups.threats)(_.pixelDistanceEdge(unit))
        .forall(t => unit.tileDistanceEdge(t) > ReaverShuttleStaticAttack.marginTiles(unit.woundPercent()))
  }

  /** Never walk onto guns that outrange us: the Reaver or the squad assignment handles those. */
  private def outranges(threat: UnitInfo, unit: FriendlyUnitInfo): Boolean =
    threat.pixelRangeAgainst(unit) > unit.pixelRangeAgainst(threat)

  /** No scarab to spend while pressured: the hull is a target, not a threat. */
  private def shotlessPressured(unit: FriendlyUnitInfo): Boolean =
    unit.scarabs == 0 && unit.matchups.threatsInRange.nonEmpty

  /** Board only for a real trip: walking distance stays on foot. */
  private def needsRide(unit: FriendlyUnitInfo): Boolean =
    ReaverShuttleFerry.needsRide(unit.pixelDistanceCenter(unit.agent.destinationNext()) * Maff.inv32)

  private def threatDist(unit: FriendlyUnitInfo): Double =
    Maff.minBy(unit.matchups.threats.filter(_.canAttack(unit)))(_.pixelDistanceEdge(unit))
      .map(unit.pixelDistanceEdge(_)).getOrElse(Double.PositiveInfinity)
}
