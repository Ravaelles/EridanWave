package Tactic

import Information.Counting.MacroCounter
import Information.Geography.Types.Base
import Lifecycle.With
import Macro.Facts.MacroFacts
import Mathematics.Maff
import Performance.Tasks.TimedTask
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Defense.{DefenseDivision, GetDefenseBase}
import Tactic.Missions.core.Mission
import Tactic.Missions.drops.{MissionAssaultFerry, MissionDTDrop, MissionSpeedlotDrop, MissionStormDrop}
import Tactic.Missions.ground.MissionKillExpansion
import Tactic.Missions.reaver.MissionReaverDrop
import Tactic.Production.core.Produce
import Tactic.Squads._
import Tactic.Tactics.Cheese._
import Tactic.Tactics.Defense._
import Tactic.Tactics.Economy._
import Tactic.Tactics.Scouting._
import Tactic.Tactics.{Tactic, TacticMeldArchons, TacticMeldDarchons}
import Utilities.?
import Utilities.Time.Minutes
import Utilities.UnitCounters.CountUpTo
import Utilities.UnitFilters._
import Utilities.UnitPreferences.PreferClose
import _root_.Tactic.Squads.Qualities.Qualities

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import Tactic.Squads.attack.SquadAttack
import Tactic.Squads.attack.SquadBackstabProxy
import Tactic.Squads.attack.SquadDarkTemplar
import Tactic.Squads.attack.SquadMindControl
import Tactic.Squads.defense.BaseGuardPost
import Tactic.Squads.defense.SquadBaseGuards
import Tactic.Squads.defense.SquadDefendBase
import Tactic.Squads.defense.SquadCatchDTs
import Tactic.Squads.defense.SquadClearExpansionBlockers
import Tactic.Squads.support.SquadAcePilots
import Tactic.Squads.support.SquadScoutExpansions

final class Tactician extends TimedTask {
  private val missions          = new ArrayBuffer[Mission]()
  private val priorityTactics   = new ArrayBuffer[Tactic]()
  private val backgroundTactics = new ArrayBuffer[Tactic]()
  private def addMission          [T <: Mission](mission: T): T = { missions          += mission; mission }
  private def addPriorityTactic   [T <: Tactic] (job: T)    : T = { priorityTactics   += job; job }
  private def addBackgroundTactic [T <: Tactic] (job: T)    : T = { backgroundTactics += job; job }

  //////////////
  // Missions //
  //////////////

  private val missionKillExpansion      = addMission(new MissionKillExpansion)
  private val missionDTDrop             = addMission(new MissionDTDrop)
  private val missionStormDrop          = addMission(new MissionStormDrop)
  // The independent Shuttle-Reaver cluster picks transports first while
  // skies are safe; SpeedlotDrop takes leftovers or a second Shuttle.
  private val missionAssaultFerry       = addMission(new MissionAssaultFerry)
  private val missionReaverDrop         = addMission(new MissionReaverDrop)
  private val missionSpeedlotDrop       = addMission(new MissionSpeedlotDrop)

  //////////////////////
  // Priority tactics //
  /////////////////////

          val workerRush                = addPriorityTactic(new WorkerRush)
          val pylonBlock                = addPriorityTactic(new TacticPylonBlock)
  private val relocateMinedOutMain      = addPriorityTactic(new TacticExpandByLiftoff)
          val produce: Produce          = addPriorityTactic(new Produce)
          val acePilots                 = addPriorityTactic(new SquadAcePilots)
  private val cannonRushMaximum         = addPriorityTactic(new MaximumCannonRush)
  private val clearBurrowedBlockers     = addPriorityTactic(new SquadClearExpansionBlockers)
  private val ejectScout                = addPriorityTactic(new TacticEjectScout)
          val scoutWithOverlord         = addPriorityTactic(new TacticInitialOverlordScout)
  private val backstabProxy             = addPriorityTactic(new SquadBackstabProxy)
  private val defendAgainstProxy        = addPriorityTactic(new DefendAgainstProxy)
  private val defendFightersAgainstRush = addPriorityTactic(new DefendFightersAgainstRush)
  private val defendAgainstWorkerRush   = addPriorityTactic(new DefendAgainstWorkerRush)
  private val defendFFEAgainst4Pool     = addPriorityTactic(new DefendFFEWithProbesEarly)
  private val makeDarkArchons           = addPriorityTactic(new TacticMeldDarchons)
  private val makeHighArchons           = addPriorityTactic(new TacticMeldArchons)
  private val mindControl               = addPriorityTactic(new SquadMindControl)
          val workerScout               = addPriorityTactic(new TacticWorkerScout)
  private val scoutForCannonRush        = addPriorityTactic(new TacticScoutForCannonRush)
          val darkTemplar               = addPriorityTactic(new SquadDarkTemplar)
  private val ralph                     = addPriorityTactic(new TacticRalph)
          val scoutExpansions           = addPriorityTactic(new SquadScoutExpansions)
          val monitor                   = addPriorityTactic(new TacticMonitor)

  //////////////////
  // Core tactics //
  //////////////////

  lazy val defenseSquads: Map[Base, SquadDefendBase] = With.geography.bases.map(base => (base, new SquadDefendBase(base))).toMap
  private val catchDTs = new SquadCatchDTs
  val attackSquad = new SquadAttack
  val baseGuards = new SquadBaseGuards

  ////////////////////////
  // Background tactics //
  ////////////////////////

  private val removeMineralBlocks       = addBackgroundTactic(new TacticRemoveMineralBlocks)
  private val gather                    = addBackgroundTactic(new TacticGather)
  private val chillOverlords            = addBackgroundTactic(new TacticOverlords)
  private val doFloatBuildings          = addBackgroundTactic(new TacticFloatBuildings)
  private val scan                      = addBackgroundTactic(new TacticScan)

  override protected def onRun(budgetMs: Long): Unit = {
    missions.foreach(_.launch())
    priorityTactics.foreach(_.launch())
    runCoreTactics()
    backgroundTactics.foreach(_.launch())
    With.squads.run(budgetMs)
  }

  private def runCoreTactics(): Unit = {
    val squadsDefending = defendDivisions()

    // Proactive DT defense
    catchDTs.launch()

    // Get freelancers
    val freelancers = (new ListBuffer[FriendlyUnitInfo] ++ With.recruiter.available.view.filter(u => IsRecruitableForCombat(u) && ! Zerg.Overlord(u)))
      .sortBy(u => (u.unitClass.isTransport, - u.frameDiscovered)) // New units first, with transports after other candidates so squads that need them can still claim them
    val freelancerCountInitial  = freelancers.size

    fillDefenseSquads(squadsDefending, freelancers)
    defendVsMutas(freelancers)
    defendVsDrops(freelancers, freelancerCountInitial)
    staffBaseGuards(freelancers)
    enforceClusterSplit()
    fillAttackSquad(freelancers, squadsDefending)
    pullDefenseWorkers(squadsDefending)
    attachSCVs()
    publishSquadBoards()
  }

  // Squad census for the trace: Alpha and Beta attack squads plus every
  // staffed home base, numbered oldest-first by town hall id. Empty
  // squads stay out so the rows only appear while they mean something.
  private def publishSquadBoards(): Unit = {
    val striking = Seq(("A", attackSquad), ("B", backstabProxy))
      .filter(_._2.units.nonEmpty)
      .map { case (name, squad) => f"$name(${squad.units.size})" }
    Debugging.CherryVis.CherryVis.board("Squads", striking.mkString(", "))

    val byAge = With.geography.ourBases.sortBy(_.townHall.map(_.id).getOrElse(Int.MaxValue)).toVector
    val guarding = defenseSquads.toVector.flatMap { case (base, squad) =>
      val count = squad.units.size
      if (count == 0) None else Some((byAge.indexOf(base) + 1, count))
    }.sortBy(_._1)
    Debugging.CherryVis.CherryVis.board("SquadsBase", guarding.map { case (number, count) => f"$number($count)" }.mkString(", "))
    Debugging.CherryVis.CherryVis.board("Contain", Micro.Actions.Protoss.Reaver.ContainStatus.now().toString)
  }

  private def defendDivisions(): Seq[(DefenseDivision, SquadDefendBase)] = {
    /////////////
    // DEFENSE //
    /////////////

    val defenseDivisions = With.battles.divisions
      .filter(d => d.bases.exists(b => (b.isOurs || b.plannedExpoRecently) && ! b.isEnemy))
      .map(new DefenseDivision(_))
      .filter(_.needsDefense)
      .sortBy(_.attackKeyDistanceTo((With.geography.home.center)))

    val squadsDefending = defenseDivisions.map(d => (d, defenseSquads(d.base))).distinct

    // Assign division to each squad
    squadsDefending.foreach(p => {
      p._2.setEnemies(p._1.enemiesOuter)
      p._2.vicinity = p._1.attackCentroidKey
    })
    squadsDefending
  }

  private def fillDefenseSquads(squadsDefending: Seq[(DefenseDivision, SquadDefendBase)], freelancers: ListBuffer[FriendlyUnitInfo]): Unit = {
    // First satisfy each defense squad
    // First pass gets essential defenders
    // Second pass gets additional defenders to be sure
    Seq(1.0, 0.75).foreach(ratio => squadsDefending.foreach(squad => Assignment.squadsPick(freelancers, Seq(squad._2), ratio)))
    // Detachments answer real attacks at fighting strength, never piecemeal.
    squadsDefending.foreach { case (_, squad) =>
      Assignment.unitsPick(
        freelancers,
        Seq(squad),
        filter = (f, s) => f.unitClass.isWarrior && s.unitsNext.count(_.unitClass.isWarrior) < ClusterSplit.minDetachment)
    }
  }

  private def defendVsMutas(freelancers: ListBuffer[FriendlyUnitInfo]): Unit = {
    // Proactive Muta defense with Archon
    if (With.scouting.enemyProximity < 0.5 && MacroFacts.enemyHasShown(Zerg.Mutalisk) && ( ! With.blackboard.wantToAttack() || ! acePilots.hasFleet)) {
      Assignment.unitsPick(
        freelancers,
        With.geography.ourBases.filter(MacroFacts.isMiningBase).map(defenseSquads(_)),
        filter = (f, s) => Protoss.Archon(f) && s.unitsNext.isEmpty)
    }
  }

  private def defendVsDrops(freelancers: ListBuffer[FriendlyUnitInfo], freelancerCountInitial: Int): Unit = {
    // Proactive drop/harassment defense
    if (With.scouting.enemyProximity < 0.5 && (
      (With.geography.ourBases.map(_.metro).distinct.size > 1 && With.frame > Minutes(10)())
      || With.unitsShown.any(Terran.Vulture, Terran.Dropship))) {

      val defenseDivisions = With.battles.divisions
        .filter(d => d.bases.exists(b => (b.isOurs || b.plannedExpoRecently) && ! b.isEnemy))
        .map(new DefenseDivision(_))
        .filter(_.needsDefense)
      val dropVulnerableBases = With.geography.ourBases.filter(b =>
        b.workerCount >= 3
        && ! defenseDivisions.exists(_.division.bases.contains(b)) // If it was in a defense division, it should have received some defenders already
        && b.metro.bases.view.flatMap(_.ourUnits).count(_.isAny(IsAll(IsComplete, IsAny(Terran.Factory, Terran.Barracks, Protoss.Gateway, IsHatchlike, Protoss.PhotonCannon, Terran.Bunker, Zerg.SunkenColony)))) < 3)

      val qualifiedClasses =
        ?(With.enemies.exists(_.isTerran),
        Seq(Terran.Marine, Terran.Vulture, Terran.Goliath, Protoss.Dragoon, Protoss.Archon, Zerg.Hydralisk, Zerg.Lurker),
        Seq(Terran.Marine, Terran.Firebat, Terran.Vulture, Terran.Goliath, Protoss.Zealot, Protoss.Dragoon, Protoss.Archon, Zerg.Zergling, Zerg.Hydralisk, Zerg.Lurker))

      Assignment.unitsPick(
        freelancers,
        dropVulnerableBases.map(defenseSquads(_)),
        filter = (f, s) => qualifiedClasses.exists(_(f)) && s.unitsNext.size < Math.min(3, freelancerCountInitial / 12))
    }
  }

  // Standing guard: from twenty warriors the army spares bodies for home
  // (air-and-ground hitters first), posted at the natural choke, answering
  // enemies at any base. Sized through the lock counter, so ablation shrinks
  // it when the army bleeds and zeroes it below twenty warriors.
  private def staffBaseGuards(freelancers: ListBuffer[FriendlyUnitInfo]): Unit = {
    val wanted = BaseGuardStaffing.guardsFor(With.units.ours.count(_.unitClass.isWarrior))
    baseGuards.lock.setCounter(CountUpTo(wanted))
    if (wanted > 0) {
      baseGuards.vicinity = BaseGuardPost()
      Assignment.unitsPick(
        freelancers,
        Seq(baseGuards),
        filter = (f, s) => f.unitClass.attacksAir && f.unitClass.attacksGround && s.unitsNext.size < wanted)
      Assignment.unitsPick(
        freelancers,
        Seq(baseGuards),
        filter = (f, s) => f.unitClass.isWarrior && s.unitsNext.size < wanted)
    }
  }

  // Cluster split: most of the army always stays main, or contains break
  // the moment home needs bodies. Trims defense squads (never their last
  // guard, never missions) back under forty-five percent delegated.
  private def enforceClusterSplit(): Unit = {
    val totalWarriors = With.units.ours.count(_.unitClass.isWarrior)
    val inAttack = attackSquad.units.count(_.unitClass.isWarrior)
    var delegated = totalWarriors - inAttack
    if ( ! ClusterSplit.overDelegated(totalWarriors, delegated)) return

    var trimmed = true
    while (trimmed && ClusterSplit.overDelegated(totalWarriors, delegated)) {
      trimmed = false
      val guardFloor = Math.min(1, BaseGuardStaffing.guardsFor(totalWarriors))
      val donors = defenseSquads.values.toVector.filter(_.unitsNext.size > 1) ++
        Vector(baseGuards).filter(_.unitsNext.size > guardFloor)
      val donor = donors
        .sortBy(-_.unitsNext.size).headOption
        .flatMap(squad => Maff.minBy(squad.unitsNext.filter(_.unitClass.isWarrior))(squad.candidateValue).map((squad, _)))
      donor.foreach { case (squad, victim) =>
        attackSquad.addUnit(victim)
        With.recruiter.lockTo(attackSquad.lock, victim)
        delegated -= 1
        trimmed = true
      }
    }
  }

  private def fillAttackSquad(freelancers: ListBuffer[FriendlyUnitInfo], squadsDefending: Seq[(DefenseDivision, SquadDefendBase)]): Unit = {
    //////////
    // FILL //
    //////////

    def freelancerValue         = freelancers.view.map(_.subjectiveValue).sum
    val freelancerValueInitial  = freelancerValue

    // If we want to attack and enough freelancers remain, populate the attack squad
    // TODO: If the attack goal is the enemy army, and we have a defense squad handling it, skip this step
    // On UMS maps there are no bases to defend and no macro call to attack,
    // so the squad would starve: units stay squadless with UmsMode termini and
    // nobody (HuntContact, detectors, formations) owns them. Staff it anyway.
    if ((With.blackboard.wantToAttack() || With.umsMode) && (With.blackboard.yoloing() || freelancerValue >= freelancerValueInitial * .7)) {
      val attackQualities = attackSquad.qualityCounter.qualitiesEnemy
      // Let's set the attack squad up for success
      if (MacroFacts.enemyHasShown(Terran.MachineShop, Terran.Vulture, Terran.SpiderMine, Terran.SiegeTankUnsieged, Terran.SiegeTankSieged)) {
        attackQualities.increaseTo(Qualities.Vulture,     (Terran.Vulture.subjectiveValue * Math.max(3, With.units.countEnemy(Terran.Vulture))).toInt)
        attackQualities.increaseTo(Qualities.SpiderMine,  3 * Terran.SpiderMine.subjectiveValue.toInt)
        attackQualities.increaseTo(Qualities.Cloaked,     3 * Terran.SpiderMine.subjectiveValue.toInt)
      }
      if (MacroFacts.enemyDarkTemplarLikely) {
        attackQualities.increaseTo(Qualities.Cloaked, Protoss.DarkTemplar.subjectiveValue.toInt)
      }
      if (MacroFacts.enemyLurkersLikely) {
        attackQualities.increaseTo(Qualities.Cloaked, Zerg.Lurker.subjectiveValue.toInt)
      }
      Assignment.unitsPick(freelancers, Seq(attackSquad))
    } else if (With.geography.ourBases.nonEmpty) {
      // No own bases on UMS maps: nothing to defend, so freelancers keep the
      // terminus UmsMode gave them instead of joining phantom defense squads.
      // Assign remaining freelancers to a defense squad.
      // If none are active, activate one to defend our entrance
      Assignment.unitsPick(freelancers, ?(
        squadsDefending.nonEmpty,
        squadsDefending.view.map(_._2),
        Maff.maxBy(Maff.orElse(
            With.blackboard.basesToHold(),
            With.geography.ourBasesAndSettlements))(_.economicValue())
          .map(GetDefenseBase(_))
          .map(defenseSquads)
          .toSeq))
    }
  }

  private def pullDefenseWorkers(squadsDefending: Seq[(DefenseDivision, SquadDefendBase)]): Unit = {
    // Pull defense workers after army assignment, before gathering claims the remainder.
    // Skipped without our own bases (ourBasesAndSettlements assumes they exist).
    if (With.geography.ourBases.nonEmpty) {
      (With.squads.next.collect { case squad: SquadDefendBase => squad } ++
        With.geography.ourBasesAndSettlements.map(GetDefenseBase(_)).distinct.map(defenseSquads))
        .distinct.foreach(squad => squad.recruitWorkers(squadsDefending.filter(_._2 == squad).flatMap(_._1.enemiesOuter).distinct))
    }
  }

  private def attachSCVs(): Unit = {
    // SCVs for Terran attacks
    if (With.self.isTerran) {
      val workers = MacroCounter.countOursComplete(Terran.SCV)
      val jobs    = With.geography.ourBases.view.map(b => 2 * b.minerals.length + 3 * b.gas.length).sum + 3
      var excess  = workers - jobs

      With.squads.next.foreach(squad =>
        if ( ! squad.vicinity.metro.exists(_.isOurs) || squad.engagedUpon || squad.engagingOn) {
          val bcs     = squad.lock.units.count(Terran.Battlecruiser)
          val tanks   = squad.lock.units.count(IsTank)
          val valks   = squad.lock.units.count(Terran.Valkyrie)
          val others  = squad.lock.units.count(_.isAny(Terran.Vulture, Terran.Goliath, Terran.Wraith))
          val scvsMin = 3 * bcs + tanks + valks
          val scvsMax = scvsMin + tanks + valks + (4 + others) / 5
          val scvs    = Maff.clamp(scvsMax, scvsMin, excess)
          squad
            .scvLock
            .setPreference(PreferClose(squad.vicinity))
            .setCounter(CountUpTo(scvs))
            .acquire()
          excess -= squad.scvLock.units.size
        })
    }
  }
}
