package Micro.Actions.Protoss.Reaver

import Lifecycle.With
import Tactic.Squads.ContainStatus
import Micro.Actions.Combat.Decisioning.Skirmish.FightOrFlee
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import ProxyBwapi.UnitTracking.AliveEnemies

/**
 * The Reaver's mission context: where it is headed and what the drop covers.
 * One of the three one-frame reads behind ReaverSituation; own state and
 * threats live in ReaverOwn and ReaverThreats.
 */
case class ReaverMission(
  hikingFar       : Boolean = false,
  isTerran        : Boolean = false,
  reaveOrdered    : Boolean = false,
  shouldFight     : Boolean = false,
  leadLanding     : Boolean = false,
  containing      : Boolean = false,
  combatNear      : Boolean = false,
  unloadVetoed    : Boolean = false,
  unloadSpotSafe  : Boolean = true,
  braveDrop       : Boolean = false)

object ReaverMission {

  /** A hike this long, in tiles, means the Reaver is commuting, not fighting. */
  val hikeTiles: Double = 15.0

  /** Guns that bear on the Shuttle from its own seat: the outer ring. */
  private def rangedShooters(foes: Iterable[UnitInfo], shuttle: FriendlyUnitInfo): Seq[UnitInfo] =
    foes.view
      .filter(_.tileDistanceEdge(shuttle) < ReaverDropBravery.rangedDist)
      .filter(foe => ReaverDropBravery.isRangedThreat(
        foe.unitClass.canAttack, foe.unitClass.melee, foe.flying, foe.canAttack(shuttle), Zerg.Scourge(foe)))
      .toVector

  def read(unit: FriendlyUnitInfo): ReaverMission = {
    val wound         = unit.woundPercent()
    val threats       = unit.matchups.threats
    val isTerranNow   = With.enemies.exists(_.isTerran)
    val containingNow = ContainStatus.now()
    val foes          = AliveEnemies.get()
    val combatNearNow = foes.exists(e =>
      e.unitClass.isCombatBuilding && ! e.flying && e.likelyStillThere &&
        e.tileDistanceEdge(unit) < ReaverContain.fightDistTiles)
    // Unload veto: ranged ground-hitters covering the landing spot inside
    // their range plus margin. Mobile guns only; statics vote elsewhere.
    val landingAt = unit.transport.map(_.pixel).getOrElse(unit.pixel)
    val gunsOnLanding = foes.count(e =>
      e.unitClass.ranged && e.unitClass.attacksGround && e.likelyStillThere &&
        e.pixelDistanceEdge(landingAt) < e.pixelRangeGround + ReaverUnloadVeto.safetyMarginTiles * 32.0)
    val firedAgo = With.framesSince(unit.lastFrameStartingAttack)
    val hasFiredShot = unit.lastFrameStartingAttack >= 0

    ReaverMission(
      hikingFar       = unit.moving && unit.pixelDistanceTravelling(unit.agent.destinationNext()) > ReaverMission.hikeTiles * 32,
      isTerran        = isTerranNow,
      reaveOrdered    = unit.agent.fightReason == FightOrFlee.Reave,
      shouldFight     = unit.agent.shouldFight,
      leadLanding     = unit.agent.ride.exists(_.agent.passengersPrioritized.headOption.exists(r =>
        Protoss.Reaver(r) && r != unit && r.agent.shouldFight && ! r.loaded && r.pixelDistanceEdge(unit) < 32)),
      containing      = containingNow,
      combatNear      = combatNearNow,
      unloadVetoed    = ReaverUnloadVeto.veto(gunsOnLanding),
      unloadSpotSafe  = ! threats.exists(t => ! t.flying && unit.tileDistanceEdge(t) < ReaverUnloadSafety.radiusTiles(wound)),
      braveDrop       = unit.transport.exists(shuttle => {
        // Engine edge distances are pixels; thresholds are tiles (rule 10).
        // shuttle.matchups.targets is empty for an unarmed hull, so guns
        // aimed at the Shuttle come off the single registry instead.
        // Scourge rides along: alive and typed, weapon data or not.
        val rangedNear = rangedShooters(foes, shuttle)
        ReaverDropBravery.decide(
          ReaverDropBravery.DropFacts(
            foesClose        = foes.count(_.tileDistanceEdge(shuttle) < ReaverDropBravery.closeDist),
            rangedNear       = rangedNear.size,
            rangedDps        = rangedNear.view.map(_.dpfAir).sum,
            tanks            = foes.count(t =>
              t.isAny(Terran.SiegeTankUnsieged, Terran.SiegeTankSieged) && t.likelyStillThere &&
                ReaverTanks.inTheater(t.tileDistanceEdge(shuttle))),
            nearStaticDefense= foes.exists(e =>
              e.unitClass.isStaticDefense && ! e.flying && e.likelyStillThere &&
                e.tileDistanceEdge(shuttle) < ReaverDropBravery.staticDefenseDist),
            // Melee inside three tiles chews a dropped Reaver on the spot.
            meleeNear        = foes.count(foe =>
              foe.tileDistanceEdge(shuttle) < ReaverDropBravery.closeMeleeDist &&
                (foe.unitClass.melee || Zerg.Scourge(foe))),
            shuttleShields   = shuttle.shieldPoints,
            reaverShields    = unit.shieldPoints,
            shuttleHitPoints = shuttle.shieldPoints + shuttle.hitPoints,
            firedAgo         = firedAgo,
            woundFraction    = wound,
            hasFired         = hasFiredShot,
            isTerran         = isTerranNow))
      }))
  }
}
