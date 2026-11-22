package Micro.Actions.Basic

import Micro.Actions.UnitControl
import Mathematics.Maff
import Mathematics.Points.Tile
import Mathematics.Shapes.Spiral
import Lifecycle.PerGame
import Lifecycle.With
import Micro.Actions.Action
import Micro.Actions.Combat.Decisioning.Skirmish.FightOrFlee
import Micro.Actions.Combat.Fight
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import Micro.Coordination.Pushing.{CircularPush, TrafficPriorities}
import Micro.Targeting.Target
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

import scala.collection.mutable
import Utilities.?
import Utilities.UnitFilters.IsWorker

object Build extends Action {
  override def isContainer: Boolean = true

  /**
   * Stalled-builder ledger (one read per frame, reset per game): failed
   * placements accumulate per builder until the base lands or the intent
   * moves on, so the shift below walks outward instead of retrying one
   * blocked tile forever. Keyed by tile index, so a reissued intent
   * starts clean without any identity bookkeeping.
   */
  private object BuildStall extends PerGame {
    private val attempts = new mutable.HashMap[Int, (Int, Int)]()

    def count(unitId: Int, tileIndex: Int, stalled: Boolean): Int = {
      val (prevTile, prevCount) = attempts.getOrElse(unitId, (tileIndex, 0))
      val next = if (stalled && prevTile == tileIndex) prevCount + 1 else 0
      attempts(unitId) = (tileIndex, next)
      next
    }

    override def reset(): Unit = attempts.clear()
  }

  /**
   * Shifted placement for a walled-out base (town halls only): when the
   * assigned tile sits under an unseen enemy (a burrowed Zergling, a
   * mine the builder is not allowed to duel) the builder re-seats the
   * base up to five tiles out instead of waiting on it forever. A
   * remembered blocker skips the wait; otherwise the failure counter
   * walks the spiral one step per stalled window. Returns None to keep
   * the assigned tile (cheap path: no search until the first shift).
   */
  private def shiftedSite(unit: FriendlyUnitInfo, tile: Tile, building: UnitClass, startNow: Boolean): Option[Tile] = {
    if ( ! building.isTownHall) return None

    val rememberedBlocker = With.units.enemy.exists(e =>
      e.alive && e.likelyStillThere && ! e.visible && ! e.flying && ! e.unitClass.isBuilding &&
        e.pixelDistanceCenter(tile.center) <= BuildShift.shiftRadiusTiles * 32.0)
    val started = tile.units.exists(u => u.isOurs && u.unitClass.isBuilding && (u.complete || u.unitClass.isProtoss))
    val atSite = unit.pixelDistanceCenter(building.tileArea.add(tile).center) < 128
    val affordable = With.self.minerals >= building.mineralPrice - 8 && With.self.gas >= building.gasPrice - 8
    val stalled = startNow && atSite && tile.visible && affordable && ! started
    val attempts = BuildStall.count(unit.id, tile.i, stalled)
    val shift = BuildShift.shiftIndex(attempts, rememberedBlocker)
    if (shift == 0) return None

    val area = building.tileArea
    val candidates = Spiral(BuildShift.shiftRadiusTiles).map(tile.add).filter(c =>
      c.valid && area.add(c).tiles.forall(_.buildableTerrain))
    candidates.lift(shift).orElse(candidates.lastOption)
  }

  /** A project already underway next to the builder: do not double-order. */
  private def inProgressNearby(unit: FriendlyUnitInfo, building: UnitClass): Boolean =
    With.units.ours.exists(u =>
      u.unitClass.isBuilding && u.unitClass == building && ! u.complete &&
        u.pixelDistanceCenter(unit.pixel) <= 10 * 32.0)

  override def allowed(unit: FriendlyUnitInfo): Boolean = unit.intent.toBuild.nonEmpty
  
  override def perform(unit: FriendlyUnitInfo): UnitControl = {
    val intent      = unit.intent.toBuildActive.get
    val building    = intent.unitClass
    val tile        = intent.tile

    // Blocked-base shift: a town hall ordered onto an unseen blocker
    // re-seats up to five tiles out instead of waiting on the spot.
    val site = shiftedSite(unit, tile, building, intent.startNow).getOrElse(tile)

    // If building already started (here or at a shifted seat), get a head start on mining
    if (unit.unitClass.isWorker && (site.units.exists(u => u.isOurs && u.unitClass.isBuilding && (u.complete || u.unitClass.isProtoss)) || inProgressNearby(unit, building))) {
      if (unit.intent.toScoutTiles.isEmpty) {
        unit.agent.toGather = Maff.minBy(With.geography.ourBases.flatMap(_.minerals))(_.pixelDistanceCenter(unit.pixel))
        Gather(unit)
      }
      return UnitControl.Claimed
    }
    
    def blockersForTile(spot: Tile) = spot.units.filter(blocker =>
      blocker != unit
      && ! blocker.unitClass.isGas
      && ! blocker.flying
      && blocker.visible)

    val buildArea             = building.tileArea.add(site)
    val ignoreBlockers        = unit.pixelDistanceTravelling(buildArea.center) > 32.0 * 8.0 || With.yolo.active
    lazy val blockersIn       = if (ignoreBlockers) Seq.empty else buildArea.tiles.flatMap(blockersForTile).toSeq
    lazy val blockersNear     = if (ignoreBlockers) Seq.empty else buildArea.expand(2, 2).tiles.flatMap(blockersForTile).toSeq
    lazy val blockersOurs     = blockersNear.filter(_.isOurs)
    lazy val blockersEnemy    = blockersNear.filter(_.isEnemy)
    lazy val blockersMineral  = blockersIn.filter(_.unitClass.isMinerals)
    lazy val blockersNeutral  = blockersIn.filter(blocker => blocker.isNeutral && ! blockersMineral.contains(blocker)).filterNot(_.invincible)
    lazy val blockersToKill   = (if (blockersEnemy.nonEmpty) blockersEnemy else blockersNeutral).filter(unit.canAttack)
    
    if (blockersMineral.nonEmpty && blockersEnemy.isEmpty) {
      unit.agent.toGather = Some(blockersMineral.head)
      Gather.delegate(unit)
    } else if(unit.intent.canFight && blockersToKill.nonEmpty) {
      lazy val noThreats  = unit.matchups.threats.isEmpty
      lazy val allWorkers = unit.matchups.threats.size == 1 && unit.matchups.threats.head.unitClass.isWorker
      lazy val healthy    = unit.totalHealth > 10 || unit.totalHealth >= unit.matchups.threats.head.totalHealth
      if (noThreats || (allWorkers && healthy) && unit.cooldownLeft < With.reaction.agencyMax) {
        Target.choose(unit)
        unit.agent.toAttack = unit.agent.toAttack.orElse(Some(blockersToKill.minBy(_.pixelDistanceEdge(unit))))
        Commander.attack(unit)
      } else if (unit.matchups.threats.exists( ! _.is(IsWorker))) {
        FightOrFlee(unit)
        Fight(unit)
      }
    }
    
    if (unit.unready) return UnitControl.Declined

    val pushPixel = buildArea.center
    Commander.defaultEscalation(unit)
    val priority = ?(unit.pixelDistanceCenter(pushPixel) < 128, TrafficPriorities.Shove, TrafficPriorities.Bump)

    With.coordinator.pushes.put(new CircularPush(priority, pushPixel, 32 + building.radialHypotenuse, unit))

    var movePixel = site.topLeftPixel.add(building.tileWidth * 16, building.tileHeight * 16)

    // Builder stages for the trace: assigned (has intent, not cleared to
    // build yet), moveToConstruct (traveling in), waiting (at the site but
    // cannot start), issueBuild:X (placing the order). Change-only logging
    // shows each transition once.
    val atSite = unit.pixelDistanceCenter(pushPixel) < 128
    lazy val affordable = With.self.minerals >= building.mineralPrice - 8 && With.self.gas >= building.gasPrice - 8
    unit.agent.act(?(atSite,
      ?(intent.startNow && site.visible && affordable, f"issueBuild:${Tactic.Missions.evaluation.KillCredits.shortName(building)}", "waiting"),
      ?(intent.startNow, "moveToConstruct", "assigned")))

    if (intent.startNow
      && site.visible
      && unit.pixelDistanceCenter(movePixel) < 112 // Addtl reference: McRave uses 96
      && affordable) {
      Commander.build(unit, building, ?(building.isAddon, site.add(4, 1), site))

      return UnitControl.Claimed
    }

    if (Protoss.Probe(unit)) {
      // If unit doesn't have to resolve collision after moving it gets back to work faster
      movePixel = movePixel.add(
        if (unit.x < movePixel.x) - unit.unitClass.dimensionRightInclusive - building.dimensionLeft - 1 else unit.unitClass.dimensionLeft + building.dimensionRightInclusive + 1,
        if (unit.y < movePixel.y) - unit.unitClass.dimensionDownInclusive  - building.dimensionUp   - 1 else unit.unitClass.dimensionUp   + building.dimensionDownInclusive + 1).walkablePixel
    } else if (Zerg.Drone(unit)) {
      // McRave found that positioning Drone (0, -7) from building location minimizes wiggling
      movePixel = movePixel.add(0, - 7)
    }

    unit.agent.decision.set(movePixel)
    if (unit.pixelDistanceTravelling(movePixel) > 256 + ?(unit.agent.path.isEmpty, 0, 256)) {
      MicroPathing.tryMovingAlongTilePath(unit, MicroPathing.getSneakyPath(unit))
    }

    if ( ! intent.startNow && unit.matchups.pixelsEntangled > -64) {
      MicroPathing.tryMovingAlongTilePath(unit, MicroPathing.getThreatAwarePath(unit, preferHome = false))
    }

    Commander.move(unit)
    UnitControl.Claimed
  }
}

