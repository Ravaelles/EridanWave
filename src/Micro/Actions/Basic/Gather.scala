package Micro.Actions.Basic

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Combat.Maneuvering.Retreat
import Micro.Actions.Combat.Maneuvering.WorkerThreatFlee
import Micro.Actions.Combat.Tactics.Potshots.Potshot
import Micro.Targeting.Target
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import Utilities.UnitFilters.IsWorker
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Strategery.Benzene
import Utilities.?
import Utilities.Time.{Minutes, Seconds}

object Gather extends Action {
  
  override def allowed(unit: FriendlyUnitInfo): Boolean = unit.agent.toGather.isDefined
  
  private val combatWindow = Seconds(2)()

  val defenseRadiusPixels = 160

  private def resourceThreatened(resource: UnitInfo): Boolean = resource.tile.enemyRangeGround > 0 || resource.tile.add(1, 0).enemyRangeGround > 0 || resource.returnTile.enemyRangeGround > 0

  private def minerThreatenedAt(miner: FriendlyUnitInfo, resource: UnitInfo): Boolean = (
    (resourceThreatened(resource) || resource.base.exists(_.enemies.exists(e => Protoss.DarkTemplar(e) && e.pixelsToGetInRange(resource) < 32 * 6)))
    && resource.base.exists(_.enemies.exists(e =>
      e.canAttackGround
      && (Zerg.Lurker(e) || ! e.burrowed)
      && e.isNone(IsWorker, Terran.Wraith, Protoss.Arbiter, Protoss.Scout, Zerg.Mutalisk)
      && e.pixelsToGetInRange(miner, resource.pixel) < defenseRadiusPixels)))
  
  override def perform(unit: FriendlyUnitInfo): Unit = {

    def resource = unit.agent.toGather.get

    // Gatherer combat micro
    if (unit.battle.nonEmpty) {

      // Move between resources if ours isn't safe to mine and we hope help will arrive
      val baseOriginal      = resource.base
      lazy val baseOpposite = baseOriginal.flatMap(b => b.naturalOf.orElse(b.natural))
      lazy val baseRemote   = Maff.minBy(With.geography.ourBases.filterNot(baseOriginal.contains))(_.heart.groundPixels(baseOriginal.map(_.heart).getOrElse(resource.tileTopLeft)))
      lazy val basePaired   = baseOpposite.orElse(baseRemote).filter(_.isOurs)
      if (minerThreatenedAt(unit, resource)) {
        var       alternativeMineralLocal   = Maff.minBy(baseOriginal .map(_.minerals.view.filterNot(minerThreatenedAt(unit, _))).getOrElse(Seq.empty))(_.pixelDistanceEdge(unit))
        lazy val  alternativeMineralRemote  = Maff.minBy(basePaired   .map(_.minerals.view.filterNot(minerThreatenedAt(unit, _))).getOrElse(Seq.empty))(_.pixelDistanceEdge(unit))
        alternativeMineralLocal = None // TODO: Be smarter about deciding when to abandon base
        unit.agent.toGather = (alternativeMineralLocal.toSeq ++ alternativeMineralRemote).find( ! minerThreatenedAt(unit, _)).orElse(Some(resource))
      }

      // Help initial scout get home
      if (With.frame < Minutes(8)() && unit.metro.exists(_.bases.exists(_.isEnemy)) && unit.matchups.threats.exists( ! _.unitClass.melee) && unit.visibleToOpponents) {
        if (With.configuration.debugging) unit.agent.act("helpScout")
        Commander.gather(unit)

        return
      }

      // Burrow from threats
      if (unit.canBurrow
        && unit.matchups.enemies.exists(enemy =>
          enemy.isNone(IsWorker, Terran.Wraith, Protoss.Arbiter, Protoss.Scout)
          && enemy.pixelDistanceEdge(unit) < enemy.pixelRangeAgainst(unit) + 32)
          && ! unit.tile.enemyDetected) {
        if (With.configuration.debugging) unit.agent.act("burrow")
        Commander.burrow(unit)
      }

      // Help with fights when appropriate, but only while a warrior is
      // still fighting nearby: when the army disengages the miner goes back
      // to work instead of trading alone.
      lazy val beckonedToFight = (
        unit.battle.isDefined
        && WorkerCombatSupport.supported(unit)
        && unit.base.exists(base =>
          base.isOurs
          && base.enemies.exists(target =>
            ! target.unitClass.isWorker
            && ! target.unitClass.isFlyer
            && ! target.effectivelyCloaked
            && ! target.isAny(Protoss.Reaver, Protoss.DarkTemplar, Zerg.Lurker, Zerg.InfestedTerran)
            && target.unitClass.attacksOrCastsOrDetectsOrTransports
            && target.pixelDistanceCenter(resource) < defenseRadiusPixels
            && unit.base.map(_.heart.center).forall(target.pixelDistanceCenter(_) < defenseRadiusPixels))))

      if (unit.totalHealth > 32 && beckonedToFight) {
        if (With.configuration.debugging) unit.agent.act("fight")
        Target.choose(unit)
        Commander.attack(unit)
      }

      // Hard threats end mining: Lurkers, Dark Templars and Vultures kill
      // workers that stay on the line, so run instead of switching
      // resources. A Mutalisk pack cannot be fought or outrun, a sieged
      // tank shelling the line outranges any answer, and a storm on the
      // worker needs no deliberation. Healthy packs still swarm (beckoned
      // above); the hurt and the outnumbered leave, preferably for
      // another base via the resource switch below.
      lazy val mutasNear = unit.matchups.threats.count(t => Zerg.Mutalisk(t) && t.pixelDistanceEdge(unit) <= 32 * WorkerThreatFlee.mutaPackTiles)
      lazy val tankShelling = unit.matchups.threats.exists(t =>
        Terran.SiegeTankSieged(t) && WorkerThreatFlee.tankShelling(unit.tileDistanceEdge(t), t.pixelRangeAgainst(unit) * Maff.inv32))
      lazy val hardThreatNear = ( ! beckonedToFight && unit.matchups.threats.exists(t =>
        (Zerg.Lurker(t) || Protoss.DarkTemplar(t) || Terran.Vulture(t)) && t.pixelDistanceEdge(unit) < defenseRadiusPixels)
        || WorkerThreatFlee.mutaPack(mutasNear) || tankShelling || unit.underStorm)
      if (hardThreatNear) {
        Retreat(unit)

        return
      }

      // Escape dangerous melee units
      lazy val meleePackNear = unit.matchups.threats.count(t => t.unitClass.melee && ! t.burrowed && t.pixelDistanceEdge(unit) < defenseRadiusPixels)
      // A weak miner never trades with a melee pack alone: at fleeHp or less
      // against two or more without a fighting warrior nearby there is no
      // winning trade, so it runs instead of standing to potshot. Worker
      // rushers opted into trading and are exempt, like in Fight.
      if (With.blackboard.workerRushers() == 0 && WorkerThreatFlee.doomedVsMelee(unit.totalHealth, meleePackNear, WorkerCombatSupport.supported(unit))) {
        Retreat(unit)

        return
      }

      lazy val stabbers        = unit.matchups.threatsInRange.filter(t => t.unitClass.melee && ! t.burrowed)
      lazy val stabbingLethal  = stabbers.map(e => ?(e.presumptiveTarget.contains(unit), 3, 2) * e.damageOnNextHitAgainst(unit)).sum >= unit.totalHealth
      // Hurt miners leave early: shields at 14 or less cannot take another
      // Zergling round, so any melee in range ends the shift.
      lazy val hurtVsMelee     = unit.unitClass.maxShields > 0 && unit.shieldPoints <= 14 && stabbers.nonEmpty
      if (unit.tile.enemyRangeGround > 0 && unit.base.isDefined && (stabbingLethal || hurtVsMelee)) {
        val drillGoal = (unit.base.get.resources ++ unit.base.flatMap(_.townHall))
          .filter(goal =>
            ((unit.carryingGas || unit.carryingMinerals)  && goal.unitClass.isTownHall  && goal.isOurs) ||
            (!unit.carryingGas                            && goal.unitClass.isGas       && goal.isOurs) ||
            (!unit.carryingMinerals                       && goal.unitClass.isMinerals))
        val bestGoal = Maff.maxBy(drillGoal)(resource => stabbers.map(stabber => resource.pixelDistanceCenter(stabber)).max)
        if (bestGoal.exists(_.unitClass.isTownHall)) {
          if (With.configuration.debugging) unit.agent.act("returnCargo")
          Commander.returnCargo(unit)
        } else if (bestGoal.isDefined) {
          // If we expect to die anyway
          if (unit.pixelDistanceEdge(bestGoal.get) < 32) {
            Potshot(unit)
          } else {
            if (With.configuration.debugging) unit.agent.act("drill")
            unit.agent.toGather = bestGoal
            Commander.gather(unit)
          }
        }
      }

      if (unit.base.exists(_.isOurs)) {
        if (With.configuration.debugging) unit.agent.act("potshot")
        Potshot(unit)
      }

      // Run away if threatened during transfer
      lazy val zoneNow        = unit.zone
      lazy val zoneTo         = resource.zone
      lazy val transferring   = ! unit.base.exists(_.owner.isUs) && zoneNow != zoneTo && ! With.geography.ourMetro.zones.contains(zoneNow) && With.geography.ourMetro.zones.contains(zoneTo)
      lazy val threatened     = unit.battle.isDefined && unit.matchups.framesOfSafety < combatWindow && ! unit.matchups.threats.forall(IsWorker)
      // Wounded miners give threats a wider berth: anybody able to hit us
      // within range plus 2.5 tiles plus 4 tiles per wound fraction.
      lazy val threatenedWounded = unit.woundPercent() > 0 && ! unit.matchups.enemiesCanAttackUs(2.5 + 4 * unit.woundPercent()).forall(IsWorker)
      lazy val threatCloser   = unit.matchups.threats.exists(_.pixelDistanceCenter(resource.pixel) < unit.pixelDistanceCenter(resource.pixel))
      if (transferring
        && (threatened || threatenedWounded)
        && threatCloser
        && (unit.visibleToOpponents || ! unit.matchups.withinSafetyMargin || unit.zone.edges.exists(_.contains(unit.pixel)))) {
        Retreat(unit)
      }
    }

    // A threatened worker belongs to the flee brain: re-issuing work
    // orders here ping-pongs with Retreat and crawls to death, one hop
    // per frame, while the engine keeps the old gather target.
    val safeToWork = unit.matchups.threatsInRange.isEmpty

    // Take safe/hidden route to expansion
    if (safeToWork && unit.metro != resource.metro && ! unit.carrying && unit.agent.decision.setAsWaypoint(MicroPathing.getWaypointAlongTilePath(unit, MicroPathing.getSneakyPath(unit, Some(resource.tile.walkableTile)))).isDefined) {
      if (With.configuration.debugging) unit.agent.act("travel")
      Commander.move(unit)
    }

    // Benzene travel hack
    if (safeToWork && Benzene() && resource.zone != unit.zone && unit.zone == With.geography.ourMain.zone) {
      if (With.configuration.debugging) unit.agent.act("travel")
      Commander.move(unit)
    }

    if (safeToWork) {
      if (With.configuration.debugging) {
        unit.agent.act(?(unit.carrying, "returnCargo", ?(resource.unitClass.isGas, "gather:Gas", "gather:Minerals")))
      }
      Commander.gather(unit)
    }
  }
}

