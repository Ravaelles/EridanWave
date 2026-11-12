package Micro.Actions.Protoss.Reaver

import Lifecycle.With
import Micro.Actions.Combat.Decisioning.Skirmish.FightOrFlee
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
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

  def read(unit: FriendlyUnitInfo): ReaverMission = {
    val wound         = unit.woundPercent()
    val threats       = unit.matchups.threats
    val isTerranNow   = With.enemies.exists(_.isTerran)
    val containingNow = ContainStatus.now()
    val combatNearNow = AliveEnemies.get().exists(e =>
      e.unitClass.isCombatBuilding && ! e.flying && e.likelyStillThere &&
        e.tileDistanceEdge(unit) < ReaverContain.fightDistTiles)
    // Unload veto: ranged ground-hitters covering the landing spot inside
    // their range plus margin. Mobile guns only; statics vote elsewhere.
    val landingAt = unit.transport.map(_.pixel).getOrElse(unit.pixel)
    val gunsOnLanding = AliveEnemies.get().count(e =>
      e.unitClass.ranged && e.unitClass.attacksGround && e.likelyStillThere &&
        e.pixelDistanceEdge(landingAt) < e.pixelRangeGround + ReaverUnloadVeto.safetyMarginTiles * 32.0)
    val firedAgo = With.framesSince(unit.lastFrameStartingAttack)
    val hasFiredShot = unit.lastFrameStartingAttack >= 0

    ReaverMission(
      hikingFar       = unit.moving && unit.pixelDistanceTravelling(unit.agent.destinationNext()) > 15 * 32,
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
        val distTiles = AliveEnemies.get().map(foe => (foe, foe.tileDistanceEdge(shuttle)))
        val foesClose = distTiles.count(_._2 < ReaverDropBravery.closeDist)
        val ranged = distTiles.filter(_._2 < ReaverDropBravery.rangedDist).filter({ case (foe, _) =>
          ReaverDropBravery.isRangedThreat(
            foe.unitClass.canAttack, foe.unitClass.melee, foe.flying, foe.canAttack(shuttle), Zerg.Scourge(foe)) })
        val rangedDps = ranged.map({ case (foe, _) => foe.dpfAir }).sum
        val tanks = AliveEnemies.get().count(t =>
          t.isAny(Terran.SiegeTankUnsieged, Terran.SiegeTankSieged) && t.likelyStillThere &&
            t.tileDistanceEdge(shuttle) < ReaverDropBravery.tankTheaterTiles)
        val nearStatics = AliveEnemies.get().exists(e =>
          e.unitClass.isStaticDefense && ! e.flying && e.likelyStillThere &&
            e.tileDistanceEdge(shuttle) < ReaverDropBravery.staticDefenseDist)
        // Melee inside three tiles chews a dropped Reaver on the spot.
        val meleeNear = distTiles.count({ case (foe, dist) => dist < ReaverDropBravery.closeMeleeDist &&
          (foe.unitClass.melee || Zerg.Scourge(foe)) })
        val shieldsUp = shuttle.shieldPoints >= ReaverDropBravery.minShields && unit.shieldPoints >= ReaverDropBravery.minShields
        val affordable = ReaverDropBravery.canAffordShot(shuttle.shieldPoints + shuttle.hitPoints, rangedDps)
        val tankGate = ! isTerranNow || tanks <= ReaverDropBravery.maxTerranTanks
        val ringsOk = ReaverDropBravery.shouldDrop(
          foesClose, ranged.size, shuttle.shieldPoints, unit.shieldPoints,
          isTerran = isTerranNow, enemyTanks = tanks, nearStaticDefense = nearStatics) && affordable
        // Stale rule: no melee at the landing, ten seconds dry, shields up.
        // Decisive on purpose - no firepower pricing here; a ranged crowd
        // past the rings still takes the drop while the gates stand.
        val staleOk = ReaverDropBravery.mayDropStale(firedAgo, wound, hasFired = hasFiredShot, meleeNear = meleeNear) &&
          shieldsUp && ! nearStatics && tankGate
        ringsOk || staleOk
      }))
  }
}
