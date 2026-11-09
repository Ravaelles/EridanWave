package Strategery

import Gameplans.All.StandardGameplan
import Lifecycle.With
import Mathematics.Maff
import Planning.Plans.{NoPlan, Plan}
import Strategery.Selection._
import Strategery.Strategies.{AllChoices, Strategy}
import bwapi.Race

import scala.collection.mutable

class Strategist extends StrategyDatabase with RecentFingerprints with GameFeatures with Rolling {

  lazy val selectedInitially: Set[Strategy] = {
    // TEMP-TEST-ZEALOTINTOGOON: force every Zerg game onto our opening while
    // testing it - delete this block to restore normal selection (no later
    // than the first evaluated mirror: keep while our PvZ record is being
    // established, remove once the opening graduates to a regular branch).
    // Both the skeleton and the flag are required: sibling skeletons
    // (proxy/cannon/Yolo) carry old gameplans that would otherwise hijack
    // the match through earlier dispatchers, so only pure 1BaseReactive
    // branches qualify; match history still chooses economy/composition.
    val forced =
      if (With.enemy.raceCurrent == Race.Zerg)
        matchBranches(Seq(Strategies.Protoss.PvZ1BaseReactive, Strategies.Protoss.PvZ1BaseZealotIntoGoon))
      else
        Seq.empty
    (if (forced.nonEmpty) forced.maxBy(_.winProbability)
     else With.configuration.playbook.policy.chooseBranch).strategies.toSet
  }

  val strategiesSelected      = new mutable.HashSet[Strategy]
  val strategiesDeselected    = new mutable.HashSet[Strategy]
  val strategiesActive        = new mutable.HashSet[Strategy]
  private val _activationRequirementsSatisfied = new mutable.HashSet[Strategy]

  private var _lastEnemyRace  = With.enemy.raceInitial
  def update(): Unit = {
    // No strategy selection on UMS micro maps: no bases, no macro game,
    // and legality evaluation assumes bases exist. The gameplan is NoPlan anyway.
    if (With.umsMode) return

    val enemyRaceNow = With.enemy.raceCurrent
    if (strategiesSelected.isEmpty) {
      if (With.configuration.fixedBuilds.nonEmpty) {
        setFixedBuild(With.configuration.fixedBuilds)
      }
      strategiesSelected ++= selectedInitially
      logOpening("initial")
    } else if (_lastEnemyRace != enemyRaceNow) {
      strategiesSelected
        .filter(_.enemyRaces.forall(r => r != Race.Unknown && r != enemyRaceNow))
        .foreach(swapOut)
    }
    _lastEnemyRace = enemyRaceNow
    strategiesSelected.foreach(activationRequirementsSatisfied)
  }

  private def activationRequirementsSatisfied(strategy: Strategy): Boolean = {
    if ( ! _activationRequirementsSatisfied.contains(strategy)
      && strategy.activationRequirements.forall(_())) {
      _activationRequirementsSatisfied += strategy
    }
    _activationRequirementsSatisfied.contains(strategy)
  }

  def isActive(strategy: Strategy): Boolean = isSelected(strategy) && strategiesActive.contains(strategy) && activationRequirementsSatisfied(strategy)
  def isSelected(strategy: Strategy): Boolean = strategiesSelected.contains(strategy)
  def activate(strategy: Strategy): Boolean = {
    // Use public "selected" to force initialization
    val output = strategiesSelected.contains(strategy)
    if (output && ! strategiesActive.contains(strategy)) {
      With.logger.debug(f"Activating strategy $strategy")
      strategiesActive += strategy
    }
    output
  }
  def deactivate(strategy: Strategy): Unit = {
    if (strategiesActive.contains(strategy)) {
      With.logger.debug(f"Deactivating strategy $strategy")
      strategiesActive -= strategy
    }
  }
  def swapIn(strategy: Strategy): Unit = {
    if ( ! strategiesSelected.contains(strategy)) {
      With.logger.debug(f"Swapping in strategy $strategy")
      System.out.println(f"OPENING (swap in): $strategy")
      strategiesSelected += strategy
      strategiesDeselected -= strategy
    }
  }
  def swapOut(strategy: Strategy): Unit = {
    if (strategiesSelected.contains(strategy)) {
      With.logger.debug(f"Swapping out strategy $strategy")
      System.out.println(f"OPENING (swap out): $strategy")
      strategiesSelected -= strategy
      strategiesDeselected += strategy
    }
    deactivate(strategy)
  }
  def swapEverything(whitelisted: Seq[Strategy], blacklisted: Seq[Strategy] = Seq.empty): Unit = {
    val matchedBranches = Maff.orElse(
      strategyBranchesLegal .filter(branch => whitelisted.forall(branch.strategies.contains)).filterNot(branch => blacklisted.exists(branch.strategies.contains)),
      strategyBranchesAll   .filter(branch => whitelisted.forall(branch.strategies.contains)).filterNot(branch => blacklisted.exists(branch.strategies.contains)))
    val bestBranch = Maff.maxBy(matchedBranches)(_.winProbability)
    bestBranch.foreach(branch => {
      strategiesSelected.filterNot(branch.strategies.contains).foreach(_.swapOut())
      branch.strategies.filterNot(strategiesSelected.contains).foreach(_.swapIn())
    })
    if (bestBranch.isEmpty) {
      With.logger.warn(f"Attempted to swap everything, but found  no legal branches. Whitelisted: $whitelisted - Blacklisted:  $blacklisted")
      blacklisted.foreach(_.swapOut())
      whitelisted.foreach(_.swapIn())
    }
  }

  private lazy val _standardGameplan = new StandardGameplan
  private var _lastStrategyWithCustomGameplan: Option[Strategy] = None
  private var _lastCustomGameplan: Option[Plan] = None
  def gameplan: Plan = {
    // UMS micro maps: no macro play. The standard gameplan would issue build
    // orders with no workers and no bases, so it stays parked.
    if (With.umsMode) return NoPlan()

    val selectedStrategyWithCustomGameplan = strategiesSelected.find(_.gameplan.isDefined)
    if (selectedStrategyWithCustomGameplan.isDefined) {
      selectedStrategyWithCustomGameplan.get.activate()
      if ( ! _lastStrategyWithCustomGameplan.contains(selectedStrategyWithCustomGameplan.get)) {
        _lastStrategyWithCustomGameplan = selectedStrategyWithCustomGameplan
        _lastCustomGameplan = _lastStrategyWithCustomGameplan.get.gameplan
      }
    } else {
      _lastStrategyWithCustomGameplan = None
      _lastCustomGameplan = None
    }
    _lastCustomGameplan.getOrElse(_standardGameplan)
  }

  private def logOpening(context: String): Unit = {
    val names = strategiesSelected.toVector.map(_.toString).sorted.mkString(" + ")
    val line = f"OPENING ($context, vs ${With.enemy.raceCurrent}): $names"
    System.out.println(line)
    With.logger.debug(line)
  }

  private def setFixedBuild(strategyNamesText: String): Unit = {    // The implementation of this is a little tricky because we have to call this before the Strategist has been instantiated

    // Get all the strategy names
    val strategyNamesLines  = strategyNamesText.replaceAll(",", " ").replaceAll("  ", " ").split("[\r\n]+").filter(_.nonEmpty).toVector
    val strategyNames       = Maff.sample(strategyNamesLines).split(" ")

    // Get all the mapped strategy objects
    val matchingBranches = AllChoices.tree
      .flatMap(ExpandStrategy.apply)
      .distinct
      .filter(branch =>
        strategyNames.forall(name =>
          branch.strategies.exists(_.toString.toLowerCase == name.toLowerCase)))

    if (matchingBranches.isEmpty) {
      With.logger.warn("Tried to use fixed build but failed to match " + strategyNamesText)
    }
    if (matchingBranches.nonEmpty) {
      With.logger.debug("Using fixed build: " + strategyNamesText)
      With.configuration.forcedPlaybook = Some(new TestingPlaybook {
        override def policy: StrategySelectionPolicy = StrategySelectionGreedy(Some(matchingBranches.map(_.strategies)))
      })
    }
  }
}


