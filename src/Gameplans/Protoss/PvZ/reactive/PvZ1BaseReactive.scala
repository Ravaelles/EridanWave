package Gameplans.Protoss.PvZ.reactive

import Gameplans.Protoss.PvZ.policy.{DefenseWithoutDragoons, EarlyPressure}
import Lifecycle.With
import Macro.Actions.Compositor
import Performance.Cache
import ProxyBwapi.Races.{Protoss, Zerg}
import Strategery.Strategies.Protoss._
import Utilities.Time.{GameTime, Minutes}
import Utilities.UnitFilters.IsWarrior
import Utilities.{?, SwapIf}

import scala.collection.mutable

class PvZ1BaseReactive extends PvZ1BaseReactiveUtilities
  with PvZReactiveOpenings
  with PvZReactiveComposition
  with PvZReactiveTech
  with PvZReactiveArmy {


  override def activated: Boolean = employing(PvZ1BaseReactive)

  protected def economy       : Economy           = ?(PvZTech(committedTech), Tech, ?(PvZExpand(committedExpand), Expand, Muscle))
  protected var timingAttack  : Boolean           = false
  protected var opening       : Opening           = Gates1012
  protected var composition   : Seq[Composition]  = Seq.empty

  protected def committedSpeedlot = haveEver(Protoss.Forge)
  protected def committedReaver   = haveEver(Protoss.Shuttle, Protoss.RoboticsSupportBay)
  protected def committedGoon     = upgradeStarted(Protoss.DragoonRange)
  protected def committedTech     = haveEver(Protoss.Assimilator)
  protected def committedExpand   = With.geography.maxMiningBasesOurs >= 2

  protected def safeVsAir: Boolean = _safeVsAir()
  private val _safeVsAir = new Cache(() => airScore >= 0)
  private def airScore: Double = {
    var output: Double = - Math.max(enemies(Zerg.Mutalisk), ?(enemyMutalisksLikely, 6, 0))
    output += 4 * units(Protoss.Archon)
    output += units(Protoss.Dragoon)
    output += 1.5 * Math.max(0d, units(Protoss.Corsair) - 2 * enemies(Zerg.Scourge))
    output

  }

  override def executeBuild(): Unit = {
    With.blackboard.acePilots.set( ! enemyHasShown(Zerg.Mutalisk))
    // Field the independent Shuttle-Reaver cluster once the crew exists;
    // the AA thresholds in the mission stand it down into the main army.
    harass(haveComplete(Protoss.Shuttle) && haveComplete(Protoss.Reaver))
    makeArchons()
    reactVs4Pool()
    chooseOpening()
    chooseComposition()
    capGas()
    if (anticipateSpeedlings) {
      openVsSpeedlings()
    } else {
      opening match {
        case Gates910   => open910()
        case Gates1012  => ?(economy == Expand, open1012Expand(), open1012Gas())
        case CoreZ      => openCoreZ()
        case ZCoreZ     => openZCoreZ()
        case ZZCoreZ    => openZZCoreZ()
        case GateNexus  => openGateNexus()
        case NexusFirst => openNexusFirst()
        case _          => open1012Gas()
      }
    }
    scoutOn(Protoss.Pylon)
  }

  protected val compositor = new Compositor

  override def executeMain(): Unit = {
    With.blackboard.scoutExpansions.set(false)

    val allIn       = allInNow
    val earlyAttack = earlyAttackAllowed
    val timing      = timingAttackNow

    aggression(?(allIn || timing, 1.5, 0.75))
    attack((allIn || earlyAttack || timing || needToPushLurkers) && ! mustDefend)
    reportStatus(allIn, earlyAttack, timing)
    With.blackboard.scoutExpansions.set(enemyBases == 0 || supplyUsed200 >= 100)

    detectionVsLurker()
    requestBases(timing)
    produceArmy()
  }

  /** Openers that can punish an early third, plus the scouts that reveal one. */
  private def earlyAttackAllowed: Boolean = {
    val earlyOpening = opened(Gates910, Gates1012) ||
      (opened(ZZCoreZ, ZCoreZ) && enemyStrategy(With.fingerprints.twelveHatch, With.fingerprints.twelvePool, With.fingerprints.overpool)) ||
      haveComplete(Protoss.Scout, Protoss.DarkTemplar)

    earlyOpening &&
      (EarlyPressure.mayPush(unitsComplete(IsWarrior)) || With.fingerprints.twelveHatch()) &&
      confidenceAttacking11 > 0.2 + 0.1 * enemiesComplete(Zerg.SunkenColony) &&
      ! enemyStrategy(With.fingerprints.fourPool, With.fingerprints.ninePoolGas, With.fingerprints.overpoolGas, With.fingerprints.tenHatchPoolGas, With.fingerprints.twoHatchMain)
  }

  /** Mutalisks without Corsairs and under six Dragoons is a base race we lose. */
  private def allInNow: Boolean =
    enemiesHave(Zerg.Mutalisk) && ! haveComplete(Protoss.Corsair) && unitsComplete(Protoss.Dragoon) < 6

  /** Lurkers we cannot see are Lurkers we cannot fight. */
  private def needToPushLurkers: Boolean =
    enemyLurkersLikely && ! haveComplete(Protoss.PhotonCannon, Protoss.Observer)

  /**
   * Timing attack: any of these is reason enough, and once true it stays
   * true - the mutalisks, the upgrades or the Reaver count that started it
   * does not have to survive.
   */
  private def timingAttackNow: Boolean = {
    timingAttack ||= unitsComplete(IsWarrior) >= 24 // Safety valve in case build gets disrupted somehow
    timingAttack ||= unitsComplete(Protoss.Reaver) >= 2     && haveComplete(Protoss.Shuttle)
    // Archon production is disabled, so the old haveComplete(Archon) conjunct
    // is gone: speed plus damage done is the timing by itself.
    timingAttack ||= upgradeComplete(Protoss.ZealotSpeed)   && upgradeComplete(Protoss.GroundDamage)
    timingAttack ||= upgradeComplete(Protoss.DragoonRange)  && unitsComplete(Protoss.Dragoon) >= 10
    timingAttack ||= haveComplete(Protoss.Scout)
    timingAttack ||= enemyMutalisksLikely
    timingAttack
  }

  /** Broke and without Dragoons the army waits at home instead of feeding. */
  private def mustDefend: Boolean =
    DefenseWithoutDragoons.shouldDefend(
      With.enemies.exists(_.isZerg),
      units(Protoss.Dragoon),
      units(Protoss.Zealot),
      With.battles.globalAttack.us.skimStrengthTotal,
      With.battles.globalAttack.enemy.skimStrengthTotal)

  private def reportStatus(allIn: Boolean, earlyAttack: Boolean, timing: Boolean): Unit = {
    status(allIn || earlyAttack && ! timing, "EarlyAttack")
    status(allIn, "AllIn")
    status(timing, "TimingAttack")
    status(economy)
    status(opening)
    status(composition.mkString(""))
    status(anticipateSpeedlings, "Speedlings")
  }

  private def requestBases(timing: Boolean): Unit = {
    requireMiningBases(?(frame > Minutes(10)(), 2, 1))
    if (timing && With.scouting.enemyProximity < 0.75 && (PvZExpand(committedExpand) || PvZMuscle())) {
      requireMiningBases(2)
    }
  }

  private def produceArmy(): Unit = {
    SwapIf(
      supplyUsed200 >= 60  - 30 * confidenceDefending01 - 4 * units(Protoss.Gateway),
      buildArmy(),
      buildTech())

    ensureCompositionTech()

    if (PvZExpand(committedExpand)) {
      get(Math.max(2, 3 - units(Protoss.RoboticsFacility, Protoss.Stargate)), Protoss.Gateway)
      requireMiningBases(2)
    }
    get(5 * miningBases - units(Protoss.RoboticsFacility, Protoss.Stargate), Protoss.Gateway)
    requireMiningBases(2) // Unlikely to happen but maybe useful if we run out of building room at home
  }

}
