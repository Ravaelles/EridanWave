package Tactic.Squads.support

import Information.Geography.Types.Base
import Lifecycle.With
import Macro.Facts.MacroFacts
import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitClasses.UnitClass
import Tactic.Missions.core.MissionDrop
import Utilities.?
import Utilities.Time.Seconds
import Utilities.UnitCounters.CountEverything
import Utilities.UnitFilters.{IsAny, IsFlyingWarrior, IsTank, IsWorker, UnitFilter}

import scala.util.Random
import Tactic.Squads.Decisions.AceExploreLogic
import Tactic.Squads.defense.SquadDefendBase
import Tactic.Squads.Squad
import Tactic.Squads.SquadAutomation
import Tactic.Squads.Squads

class SquadAcePilots extends Squad {
  val acePilots         : Seq[UnitClass]  = Seq(Protoss.Corsair) //Seq(Terran.Wraith, Terran.Valkyrie, Protoss.Corsair, Zerg.Mutalisk, Zerg.Scourge)
  val acePilotMatcher   : UnitFilter      = IsAny(acePilots: _*)
  val splashPilots      : Seq[UnitClass]  = Seq(Terran.Valkyrie, Protoss.Corsair)
  val splashPilotMatcher: UnitFilter      = IsAny(splashPilots: _*)

  lock.matcher = acePilotMatcher
  lock.counter = CountEverything
  override def launch(): Unit = {
    if ( ! With.blackboard.acePilots()) return
    if (With.yolo.active) return

    lock.acquire()
  }

  var activity: String = toString
  var hasFleet: Boolean = false
  override def toString: String = activity

  override def run(): Unit = {
    val lastActivity = activity
    chooseActivity()
    if (activity != lastActivity) {
      With.logger.debug(f"Pilots switch from $lastActivity to $activity")
    }
    holdFreshAttackers()
    Micro.Corsair.CorsairBrain.act(this)
  }

  /**
   * Fresh shields while attacking: hold position instead of fleeing ground
   * fire (FightOrFlee CantFlee forces the fight). Past 16 shield damage the
   * normal flee logic resumes next frame - canFlee is set both ways here,
   * so no stale intent survives.
   */
  private def holdFreshAttackers(): Unit = {
    val attacking = targets.exists(_.nonEmpty)
    units.foreach(u => u.intend(this).setCanFlee( ! (attacking && u.unitClass.maxShields - u.shieldPoints <= 16)))
  }

  private def chooseActivity(): Unit = {
    val otherSquads   = With.squads.all.view.filterNot(==)
    val weSplash      = units.exists(splashPilotMatcher)
    val enemySplashes = With.units.enemy.exists(splashPilotMatcher)
    val requireFleet  = MacroFacts.enemyHasShown(acePilots: _*) || MacroFacts.enemiesCompleteFor(Zerg.Scourge.buildFrames, Zerg.Spire) > 0
    hasFleet = units.size >= Math.max(6, if (weSplash && ! enemySplashes) 0 else MacroFacts.enemies(acePilots: _*))

    // Help other squads with anti-air
    val squadsToCover = otherSquads.filter(_.targets.exists(_.exists(IsFlyingWarrior))).toVector
    if (squadsToCover.nonEmpty) {
      val squad = squadsToCover
        .sortBy( ! _.isInstanceOf[SquadDefendBase])
        .maxBy(_.targets.get.count(IsFlyingWarrior))
      activity = "AceHelpSquad"
      followSquad(squad)

      return
    }

    // If they have fast fliers,
    // and we don't have enough to contend against them,
    // seek shelter with the ground army

    if (requireFleet && ! hasFleet) {
      activity = "AceChill"
      chill()

      return
    }

    // Aggressively engage air divisions
    val airDivisions = With.battles.divisions
      .view
      .filter(_.enemies.exists(e => IsFlyingWarrior(e) && With.framesSince(e.lastSeen) < Seconds(10)()))
      .filter(d => hasFleet || units.size > d.enemies.count(IsFlyingWarrior))
    val airDivision = airDivisions
      .sortBy(d => d.centroidAir.pixelDistanceSquared(centroidAir))
      .sortBy(d => ?(weSplash, -d.enemies.size, 1))
      .headOption
    if (airDivision.isDefined) {
      activity = "AceAir2Air"
      vicinity = airDivision.get.centroidAir
      SquadAutomation.targetAndSend(this)

      return
    }

    // Help clear detection
    val squadsFacingDetection = otherSquads.filter(s => s.units.exists(u => Protoss.DarkTemplar(u) && u.matchups.groupVs.detectors.nonEmpty && u.matchups.groupVs.detectors.forall(_.flying))).toVector
    if (squadsFacingDetection.nonEmpty) {
      activity = "AceCloakDT"
      followSquad(squadsFacingDetection.minBy(_.centroidAir.pixelDistanceSquared(centroidAir)))

      return
    }

    // Help drop missions
    val dropMissions = otherSquads.filter(_.isInstanceOf[MissionDrop]).map(_.asInstanceOf[MissionDrop])
    if (dropMissions.nonEmpty) {
      activity = "AceEscortDrop"
      followSquad(dropMissions.maxBy(_.duration))

      return
    }

    // Unleash hell!
    if (units.exists(_.isAny(Zerg.Mutalisk, Protoss.Scout) && With.geography.enemyBases.nonEmpty)) {
      val bombingBases = With.geography.enemyBases.map(b => (
        b,
        b.enemies.view.filter(_.isAny(IsWorker, IsTank, Terran.ScienceVessel, Protoss.HighTemplar, Protoss.Shuttle, Protoss.Reaver)).map(_.subjectiveValue).sum,
        b.enemies.view.filter(_.canAttackAir).map(u => u.subjectiveValue * Maff.or1(4, u.isAny(Terran.MissileTurret, Protoss.Archon, Zerg.SporeColony))).sum,
        b.heart.center.pixelDistance(centroidAir)))
      val bombingBaseScores = bombingBases.map(b => (b._1, b._2 / (1 + b._3) / (640 + b._4)))
      val bombingTarget     = Maff.maxBy(bombingBaseScores)(_._2)
      if (bombingTarget.isDefined) {
        activity = "Bombing"
        vicinity = bombingTarget.get._1.heart.center
        SquadAutomation.targetAndSend(this)

        return
      }
    }

    // Where to look when a visited base shows nothing: known enemy bases
    // holding enemies, most enemies first, nearest to break ties.
    val likelyBases = likeliestEnemyBases

    // Monitor their bases. A random stale base first; an emptied visit falls
    // back to the likeliest known enemy base instead of wandering the map.
    // Don't look to target anything; just get scouting information first
    val unscoutedEnemy = getStaleBase(With.geography.enemyBases, 120)
    val monitorPick = AceExploreLogic.pick(
      current            = vicinity.base.map(_.name),
      currentScouted     = vicinity.base.exists(b => With.framesSince(b.lastFrameScoutedByUs) < Seconds(15)()),
      currentHasEnemies  = vicinity.base.exists(_.enemies.nonEmpty),
      stale              = unscoutedEnemy.map(_.name),
      fallback           = likelyBases.map(_.name),
      roll               = Random.nextInt())
    if (monitorPick.isDefined) {
      activity  = "AceMonitor"
      val base  = (unscoutedEnemy ++ likelyBases).find(_.name == monitorPick.get).get
      vicinity  = base.townHallArea.center
      setTargets(Iterable.empty)
      SquadAutomation.send(this)

      return
    }

    // Just snipe anything we can
    val nearestFlier = Maff.minBy(With.units.enemy.filter(u => u.likelyStillThere && u.flying && ! u.unitClass.isBuilding))(_.pixelDistanceSquared(centroidAir))
    if (nearestFlier.isDefined) {
      activity = "AceHunt"
      vicinity = nearestFlier.get.pixel
      SquadAutomation.targetAndSend(this)

      return
    }

    // Explore, and snipe. Prefer bases without static anti-air: a Corsair
    // circling a Spore Colony is a dead Corsair, so those bases are only
    // visited when nothing else is stale. A random stale base first; an
    // emptied visit falls back to the likeliest known enemy base.
    val staleBases = Maff.orElse(
      getStaleBase(With.geography.enemyBases,   30),
      getStaleBase(With.geography.neutralBases, 30),
      getStaleBase(With.geography.enemyBases,   10)).toVector
    val basesWithoutStaticAA = staleBases.filterNot(b =>
      b.enemies.exists(e => e.unitClass.isStaticDefense && e.canAttackAir))
    val explorePool = if (basesWithoutStaticAA.nonEmpty) basesWithoutStaticAA else staleBases
    val explorePick = AceExploreLogic.pick(
      current            = vicinity.base.map(_.name),
      currentScouted     = vicinity.base.exists(b => With.framesSince(b.lastFrameScoutedByUs) < Seconds(15)()),
      currentHasEnemies  = vicinity.base.exists(_.enemies.nonEmpty),
      stale              = explorePool.map(_.name),
      fallback           = likelyBases.map(_.name),
      roll               = Random.nextInt())
    if (explorePick.isDefined) {
      activity = "AceExplore"
      val base = (explorePool ++ likelyBases).find(_.name == explorePick.get).get
      vicinity = base.townHallArea.center
      SquadAutomation.targetAndSend(this)

      return
    }

    chill()
  }

  private def getStaleBase(bases: Seq[Base], thresholdSeconds: Int): Seq[Base] = {
    bases.filter(b =>
      With.framesSince(b.lastFrameScoutedByUs) > Seconds(thresholdSeconds)()
      && ! b.enemies.exists(_.canAttackAir))
  }

  // Likeliest known enemy bases, best first: most enemies holding, nearest
  // to break ties. No static anti-air first, anything second, the known
  // main last - a Corsair circling a Spore Colony is a dead Corsair.
  private def likeliestEnemyBases: Seq[Base] = {
    val holding = With.geography.enemyBases.filter(_.enemies.nonEmpty)
    def ordered(bases: Seq[Base]): Seq[Base] = bases.sortBy(b =>
      (-b.enemies.size, b.townHallArea.center.pixelDistanceSquared(centroidAir)))

    Maff.orElse(
      ordered(holding.filterNot(b =>
        b.enemies.exists(e => e.unitClass.isStaticDefense && e.canAttackAir))),
      ordered(holding),
      With.scouting.enemyMain.toSeq).toVector
  }

  private def chill(): Unit = {
    activity = "AceChill"
    val groundToAir = Maff.exemplarOpt(With.units.ours.filter(u => u.canAttackAir && ! u.flying).map(_.pixel))
    vicinity = groundToAir.getOrElse(homeConsensus)
    SquadAutomation.target(this)
    units.foreach(_.intend(this).setTerminus(vicinity).setRedoubt(vicinity))
  }

  private def followSquad(otherSquad: Squad): Unit = {
    vicinity = otherSquad.vicinity
    val base = vicinity.base
    val targetsUnsorted = Maff.orElse(
      otherSquad.targets.map(_.filter(_.flying)).getOrElse(Seq.empty),
      base.map(_.enemies.filter(_.flying)).getOrElse(Seq.empty),
      SquadAutomation.unrankedAround(this, vicinity))
    setTargets(targetsUnsorted.toVector.sortBy(_.pixelDistanceSquared(base.map(_.heart.center).getOrElse(vicinity))))
    SquadAutomation.send(this)
  }
}

