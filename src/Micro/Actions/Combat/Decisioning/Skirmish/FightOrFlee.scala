
package Micro.Actions.Combat.Decisioning.Skirmish

import Micro.Actions.UnitControl
import Debugging.Probe
import Lifecycle.{Enemy, With}
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Protoss.Dragoon.DragoonPvpRetreat
import Micro.Targeting.CarrierTankHuntSeam
import Micro.Targeting.FiltersRequired.TargetFilterScout
import Tactic.Squads.Decisions.ClusterPosture
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.?
import Utilities.Time.Minutes
import Utilities.UnitFilters.{IsSlowling, IsTank, IsWarrior, IsWorker}

object FightOrFlee extends Action {
  override def unitSubset: Micro.Actions.UnitSubset = Micro.Actions.UnitSubsets.AllUnits
  override def isContainer: Boolean = true

  val Reave = "Reave"

  override def allowed(unit: FriendlyUnitInfo): Boolean = unit.canMove

  //noinspection NameBooleanParameters
  override def perform(u: FriendlyUnitInfo): UnitControl = {
    applyVerdict(u, gatherFacts(u))

    if (With.configuration.debugging && With.configuration.logCombatForecasts) Debugging.CombatForecastDiagnostics.decision(u)
    UnitControl.Declined
  }

  // The verdict reason (rule label or sim Yes/No) rides the change-only
  // Verdict probe so traceless games can answer "why is it retreating".
  private def applyVerdict(u: FriendlyUnitInfo, facts: FightFacts): Unit = {
    FightRules.decide(facts) match {
      case Some((fight, reason)) =>
        u.agent.decideFight(fight, reason)
        Probe.note("Verdict", u.id, ?(fight, "+", "-") + reason)
        if (With.configuration.formationAudit) Debugging.FormationAudit.tallyVerdict(?(fight, "+", "-") + reason)
      case None =>
        applyEstimation(u)
        Probe.note("Verdict", u.id, ?(u.agent.shouldFight, "+Sim", "-Sim") + ":" + u.agent.fightReason)
        if (With.configuration.formationAudit) Debugging.FormationAudit.tallyVerdict(?(u.agent.shouldFight, "+Sim", "-Sim"))
    }
  }

  private def gatherFacts(u: FriendlyUnitInfo): FightFacts =
    errandsFacts(u, ordnanceFacts(u, cloakFacts(u, contactFacts(u, personalFacts(u)))))

  private def personalFacts(u: FriendlyUnitInfo): FightFacts = FightFacts(
    pacifist    = ! u.canAttack && ! u.unitClass.castsSpells,
    cantFight   = ! u.squadIntent.canFight,
    cantFlee    = ! u.squadIntent.canFlee,
    static      = ! u.canMove,
    committed   = u.agent.commit,
    hallTargeted = u.agent.toAttack.exists(_.unitClass.isTownHall),
    loneScout = Protoss.Scout(u) && Enemy.protoss() && ! TargetFilterScout.hasWingmate(u),
    outmatchedGoon = Protoss.Dragoon(u) && Enemy.protoss() && {
      val global = With.battles.globalAttack
      DragoonPvpRetreat.shouldRetreat(
        true,
        true,
        With.units.ours.count(o => o.alive && Protoss.Dragoon(o)),
        With.units.enemy.exists(e => e.alive && Protoss.Dragoon(e)),
        global.us.units.count(_.unitClass.isWarrior).toDouble,
        global.enemy.units.count(_.unitClass.isWarrior).toDouble)
    },
    tankHunt    = Protoss.Carrier(u) && CarrierTankHuntSeam.huntFor(u),
    carrierBrawl = Protoss.Carrier(u) && CarrierTankHuntSeam.brawlingFor(u),
    yolo        = With.yolo.active && IsWarrior(u),
    irradiated  = u.painfullyIrradiated,
    dragoonBrave  = NoRetreat.dragoonBrave(u),
    dragoonPack   = NoRetreat.dragoonPack(u),
    homeStand     = NoRetreat.homeStand(u),
    tanksAtNatural = NoRetreat.tanksAtNatural(u),
    reaverBrave   = NoRetreat.reaverBrave(u))
  private def contactFacts(u: FriendlyUnitInfo, base: FightFacts): FightFacts = base.copy(
    safe        = u.battle.exists(! _.enemy.canAttackIfVisible(u)),
    // Cluster-level contact: no enemy within firing range plus margin of ANY
    // squad unit means the fight is over - disengage unconditionally and
    // reform instead of milling around scattered. Falls back to the lone
    // unit when it has no squad.
    outOfContact = ! u.squad.map(_.units).getOrElse(Seq(u)).exists(_.canAnyEnemyAttackUsWithMargin(FightRules.contactMarginTiles)),
    detonated   = u.unitClass.suicides && u.matchups.targetsInRange.exists(_.canAttack(u)),
    hugTank     = ! u.flying && u.agent.ride.isEmpty && With.enemies.exists(_.isTerran) && u.matchups.targetsInRange.exists(t => Terran.SiegeTankSieged(t) && t.visible && t.canAttack(u)),
    archon      = Protoss.Archon(u) && u.matchups.targetsInRange.exists(_.unitClass.attacksOrCastsOrDetectsOrTransports),
    coverFire   = u.unitClass.supportsAnchor && u.combatAllies.exists(ally => ! ally.loaded && ally.unitClass.isAnchor && u.matchups.threatDeepest.exists(enemy => u.canAttack(enemy) && ally.canAttack(enemy) && enemy.pixelsToGetInRange(ally) <= Math.min(32 * 8, 16 + u.pixelsToGetInRange(enemy)) && u.pixelDistanceEdge(ally) < 32 * 10)),
    berzerk     = With.frame < Minutes(6)() && u.isAny(Protoss.Zealot, IsSlowling) && ! u.team.exists(_.catchesGround) && u.matchups.threatDeepest.exists(td => Terran.Vulture(td) && td.matchups.targetNearest.exists(tn => td.pixelsToGetInRange(tn) < 32 && tn.unitClass == u.unitClass)),
    // Outgunned under cover: an enemy static that can already hit us, local
    // warriors outnumbering ours, and no reach edge over the nearest one -
    // standing in trades the hull for the cover's DPS (BetaStar: 1 goon
    // into 2 goons under cannons). Range advantage still fights.
    staticOutgunned = staticCovered(u) && locallyOutnumbered(u) && ! outrangesNearest(u),
    // Sunken trap: a blade inside the colony's reach cannot trade with
    // it at all - the pack math above needs outnumbering, but a solo
    // hull dies the same, so it walks home instead of planting volleys
    // against masonry (PvZ KiteHold/March flap till death).
    sunkenTrap = FightRules.sunkenTrap(u.unitClass.melee, sunkenNear(u)))

  private def staticCovered(u: FriendlyUnitInfo): Boolean =
    u.unitClass.isWarrior && ! u.flying && u.matchups.enemies.exists(e =>
      e.alive && e.likelyStillThere && e.unitClass.isStaticDefense && e.canAttack(u) &&
      u.pixelDistanceEdge(e) <= e.pixelRangeAgainst(u) + FightRules.staticCoverMarginTiles * 32.0)

  /** Nearest live Sunken that can hit us, in tiles - the trap the blade walks home from. */
  private def sunkenNear(u: FriendlyUnitInfo): Double =
    Maff.minBy(u.matchups.enemies.view.filter(e =>
      e.alive && e.likelyStillThere && Zerg.SunkenColony(e) && e.canAttack(u)))(
      u.pixelDistanceEdge(_)).map(u.pixelDistanceEdge(_) * Maff.inv32).getOrElse(Double.PositiveInfinity)

  private def locallyOutnumbered(u: FriendlyUnitInfo): Boolean = {
    // allies excludes self by construction, so the hull counts itself.
    val foes = u.matchups.threats.count(t => t.alive && t.unitClass.isWarrior && t.canAttack(u))
    val ours = u.matchups.allies.count(a => a.alive && a.unitClass.isWarrior) + 1
    foes > ours
  }

  private def outrangesNearest(u: FriendlyUnitInfo): Boolean =
    Maff.minBy(u.matchups.threats.view.filter(t => t.alive && t.unitClass.isWarrior && t.canAttack(u)))(
      u.pixelDistanceEdge(_)).exists(foe =>
      u.pixelRangeAgainst(foe) > foe.pixelRangeAgainst(u))

  private def cloakFacts(u: FriendlyUnitInfo, base: FightFacts): FightFacts = base.copy(
    cloaked     = u.effectivelyCloaked && u.isAny(Terran.Wraith, Protoss.DarkTemplar) && ( ! Terran.Wraith(u) || u.energy > 10),
    cloakable   = Terran.Wraith(u) && Terran.WraithCloak() && u.energy >= 50 && u.matchups.groupVs.detectors.isEmpty,
    lurking     = Zerg.Lurker(u) && u.matchups.groupVs.detectors.isEmpty,
    towers      = With.frame < Minutes(8)() && u.matchups.allies.exists(a => a.unitClass.isBuilding && a.unitClass.canAttack && (a.matchups.targetNearest.exists(t => u.canAttack(t) && a.inRangeToAttack(t)) || a.matchups.threatNearest.exists(t => u.canAttack(t) && t.canAttack(a) && t.framesToGetInRange(a) <= u.framesToGetInRange(a) && ((t.tileDistanceEdge(a) < 6) || t.inRangeToAttack(a))))),
    scourge     = u.flying && With.enemies.exists(_.isZerg) && u.matchups.groupVs.has(Zerg.Scourge) && u.matchups.threats.exists(t => Zerg.Scourge(t) && t.pixelDistanceEdge(u) < 128 && t.matchups.threatsInRange.map(_.damageOnNextHitAgainst(t)).sum < t.hitPoints))

  private def ordnanceFacts(u: FriendlyUnitInfo, base: FightFacts): FightFacts = base.copy(
    scarabless  = Protoss.Reaver(u) && u.scarabs == 0 && ! u.trainee.exists(_.remainingCompletionFrames < Math.max(u.matchups.framesOfSafety, u.cooldownLeft)),
    birdless    = Protoss.Carrier(u) && ! u.interceptors.exists(_.complete),
    noEnergy    = ! u.canAttack && u.energyMax > 0 && u.unitClass.spells.forall(s => s.energyCost > u.energy || ! With.self.hasTech(s)),
    disrupted   = ! u.flying && u.underDisruptionWeb && u.matchups.groupVs.canAttack(u),
    bideSiege   = Terran.SiegeTankUnsieged(u) && ! With.blackboard.wantToAttack() && ! With.self.hasTech(Terran.SiegeMode) && u.matchups.groupVs.has(Protoss.Dragoon) && With.units.ours.exists(_.techProducing.contains(Terran.SiegeMode)) && u.alliesBattle.exists(a => Terran.Bunker(a) && a.complete),
    carrierVuln = Protoss.Carrier(u) && u.matchups.threats.exists(t => ! t.flying && t.pixelsToGetInRange(u) < 32))

  private def errandsFacts(u: FriendlyUnitInfo, base: FightFacts): FightFacts = base.copy(
    raze        = ((IsTank(u) && Terran.SiegeMode()) || u.isAny(Protoss.Reaver, Zerg.Guardian)) && u.matchups.threats.forall(t => t.unitClass.isWorker || t.unitClass.isBuilding),
    miners      = ! u.isAny(IsWorker, Protoss.Carrier) && u.canAttack && u.battle.exists(_.us.minersThreatened) && u.presumptiveTarget.exists(t => t.isEnemyOf(u) && t.matchups.threateningMiners),
    reave       = Protoss.Reaver(u) && u.scarabs > 0 && u.agent.ride.isDefined && u.confidence01 > 0.45 - Maff.or0(0.05, u.loaded) - Maff.or0(0.1, Protoss.ShuttleSpeed()) && u.matchups.threatNearest.exists(t => u.canAttack(t) && t.speedApproaching(u) > Protoss.Reaver.topSpeed) && (u.matchups.framesOfSafety >= ?(u.loaded, 30, 0) && (u.loaded || u.cooldownLeft <= 31)),
    energized   = u.unitClass.maxShields > 20
      && With.frame < Minutes(10)()
      && u.alliesBattle.exists(ally =>
        Protoss.ShieldBattery(ally)
        && ally.complete
        && ally.energy > 20
        && ally.pixelDistanceEdge(u, u.presumptiveTarget.map(u.pixelToFireAtSimple).getOrElse(u.pixel)) < 72))

  private def applyEstimation(unit: FriendlyUnitInfo): Unit = {
    unit.squad.flatMap(_.battlePosture) match {
      case Some(posture) =>
        // The cluster already voted from these same sims: SquadAttack feeds
        // the fighting fraction and the penalized advantage into
        // ClusterPosture.next, so re-asking each sim here would only
        // re-split the vote it just settled. Obey the posture instead.
        // Explicit rules above (commitments first) already spoke for units
        // with their own orders; this fallback serves the line.
        val fight = ClusterPosture.advances(posture)
        unit.agent.decideFight(fight, ?(fight, "Posture", "PostureHold"))
      case None =>
        if (unit.combatForecast.isEmpty) {
          // Important for things which care about unit willingness to trot around
          unit.agent.decideFight(true, "")
        } else {
          val simFight = unit.combatForecast.get.judgement.get.unitShouldFight(unit)
          unit.agent.decideFight(simFight, ?(simFight, "Yes", "No"))
        }
    }
  }
}
