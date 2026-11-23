package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

/**
 * March gate order table: first claim wins, in order. Every row pins
 * which gate a (world facts, per-unit facts) pair selects - especially
 * the pairs where two gates fire and the earlier one must own the frame.
 * Latches keep per-unit state, so every row hunts with a fresh unit id
 * and reads the sharp (first-frame) side, exactly like a unit the march
 * meets for the first time.
 */
class MarchGateOrderSuite extends AnyFunSuite {

  private def facts(
    enemyIsTerran    : Boolean = false,
    enemyIsProtoss   : Boolean = false,
    enemyHasTanks    : Boolean = false,
    enemyHasVultures : Boolean = false,
    enemyDragoonSeen : Boolean = false,
    bankedMinerals   : Int = 0,
    armyStrength     : Double = 0.0,
    armyUnitsScore   : Double = 0.0,
    supplyUsed       : Int = 100,
    proxyRush        : Boolean = false,
    home             : Boolean = true): MarchFacts =
    MarchFacts(
      enemyIsTerran, enemyIsProtoss, enemyHasTanks, enemyHasVultures, enemyDragoonSeen,
      bankedMinerals, armyStrength, armyUnitsScore, supplyUsed, proxyRush,
      home = if (home) Some(null) else None,
      catchUpAnchor = None,
      alphaAnchorTiles = Double.PositiveInfinity)

  private var nextId: Int = 1000

  private def unit(
    isDarkTemplar       : Boolean = false,
    isCorsair           : Boolean = false,
    isDragoon           : Boolean = false,
    isWarrior           : Boolean = false,
    isLoadedShuttle     : Boolean = false,
    isSuicide           : Boolean = false,
    shouldFight         : Boolean = false,
    verdictRetreat      : Boolean = false,
    verdictDuel         : Boolean = false,
    hasDuel             : Boolean = false,
    nearestWarriorTiles : Option[Double] = None,
    distHomeTiles       : Option[Double] = None,
    nearestArmedTiles   : Double = Double.PositiveInfinity,
    nearestStaticTiles  : Option[Double] = None,
    nearestVisibleArmedTiles : Double = Double.PositiveInfinity,
    behindAlphaTiles    : Double = Double.NegativeInfinity,
    hasContact          : Boolean = false): MarchUnit = {
    nextId += 1
    MarchUnit(
      nextId, isDarkTemplar, isCorsair, isDragoon, isWarrior, isLoadedShuttle,
      isSuicide, shouldFight, verdictRetreat, verdictDuel, hasDuel, nearestWarriorTiles, distHomeTiles, nearestArmedTiles, nearestStaticTiles, nearestVisibleArmedTiles, behindAlphaTiles, hasContact)
  }

  private def check(name: String, f: MarchFacts, u: MarchUnit, want: Option[MarchLabel]): Unit =
    test(name) {
      assert(MarchGates.select(f, u) == want)
    }

  check("a Dark Templar outranks every other gate",
    facts(enemyIsTerran = true, enemyHasTanks = true, enemyHasVultures = true,
      bankedMinerals = 5000, armyStrength = 300.0, supplyUsed = 200),
    unit(isDarkTemplar = true, isSuicide = true, isWarrior = true, shouldFight = true,
      nearestStaticTiles = Some(5.0)),
    Some(MarchLabel.DarkTemplarHunt))

  check("a Dark Templar hunts without a fight consensus",
    facts(),
    unit(isDarkTemplar = true, shouldFight = false),
    Some(MarchLabel.DarkTemplarHunt))

  check("a Corsair outranks a suicide commit",
    facts(),
    unit(isCorsair = true, isSuicide = true, shouldFight = true),
    Some(MarchLabel.CorsairHunt))

  check("the main march skips the dance at home past 90 supply",
    facts(supplyUsed = 100),
    unit(isWarrior = true, distHomeTiles = Some(5.0)),
    Some(MarchLabel.MainMarch))

  check("no bases means no doorstep even maxed",
    facts(supplyUsed = 200, home = false),
    unit(isWarrior = true, distHomeTiles = None),
    None)

  check("a suicide commit outranks a banked stock",
    facts(bankedMinerals = 5000, armyStrength = 300.0, supplyUsed = 200),
    unit(isSuicide = true, isWarrior = true, shouldFight = true),
    Some(MarchLabel.SuicideMission))

  check("a loaded Shuttle outranks a banked stock",
    facts(bankedMinerals = 5000, armyStrength = 300.0, supplyUsed = 200),
    unit(isLoadedShuttle = true, shouldFight = true),
    Some(MarchLabel.PairRide))

  check("a lone hunter without catch-up marches the legs",
    facts(enemyIsProtoss = true, enemyDragoonSeen = true),
    unit(isWarrior = true, shouldFight = true,
      nearestWarriorTiles = None, nearestStaticTiles = Some(5.0)),
    None)

  check("a lone duelist holds instead of marching at the goal",
    facts(enemyIsProtoss = true, enemyDragoonSeen = true),
    unit(isWarrior = true, shouldFight = true, hasDuel = true, verdictDuel = true,
      nearestWarriorTiles = None, nearestStaticTiles = Some(5.0)),
    None)

  check("the gate reads the latched verdict, never the fresh duel",
    facts(enemyIsProtoss = true, enemyDragoonSeen = true),
    unit(isWarrior = true, shouldFight = true, hasDuel = true, verdictDuel = false,
      nearestWarriorTiles = None, nearestStaticTiles = Some(5.0)),
    None)

  check("a latched duel blocks the lone march through a target flicker",
    facts(enemyIsProtoss = true, enemyDragoonSeen = true),
    unit(isWarrior = true, shouldFight = true, hasDuel = false, verdictDuel = true,
      nearestWarriorTiles = None, nearestStaticTiles = Some(5.0)),
    None)

  check("a lone retreating hull walks home, not at the goal",
    facts(enemyIsProtoss = true, enemyDragoonSeen = true),
    unit(isWarrior = true, shouldFight = true, verdictRetreat = true,
      nearestWarriorTiles = None),
    None)

  check("a lone hull holds against a visible gun inside the cordon",
    facts(enemyIsProtoss = true, enemyDragoonSeen = true),
    unit(isWarrior = true, shouldFight = true,
      nearestWarriorTiles = None, nearestVisibleArmedTiles = 10.0),
    None)

  check("distant guns let the catch-up through",
    facts(enemyIsProtoss = true, enemyDragoonSeen = true),
    unit(isWarrior = true, shouldFight = true,
      nearestWarriorTiles = Some(3.0), nearestVisibleArmedTiles = 30.0, behindAlphaTiles = 10.0),
    Some(MarchLabel.CatchUp))

  check("a packed hunter with no fight consensus marches the legs",
    facts(enemyIsProtoss = true, enemyDragoonSeen = true),
    unit(isWarrior = true, shouldFight = false,
      nearestWarriorTiles = Some(3.0), nearestStaticTiles = Some(5.0)),
    None)

  check("a reinforcement trailing the alpha outranks the doorstep march",
    facts(supplyUsed = 100),
    unit(isWarrior = true, shouldFight = true, distHomeTiles = Some(5.0),
      nearestWarriorTiles = Some(3.0), nearestVisibleArmedTiles = 30.0, behindAlphaTiles = 10.0),
    Some(MarchLabel.CatchUp))

  check("a far duel never pins a catching-up reinforcement",
    facts(enemyIsProtoss = true, enemyDragoonSeen = true),
    unit(isWarrior = true, shouldFight = true, hasDuel = true, verdictDuel = true,
      nearestWarriorTiles = Some(3.0), nearestVisibleArmedTiles = 30.0, behindAlphaTiles = 10.0),
    Some(MarchLabel.CatchUp))

  check("contact owns the frame even behind the alpha",
    facts(enemyIsProtoss = true, enemyDragoonSeen = true),
    unit(isWarrior = true, shouldFight = true, hasDuel = true, verdictDuel = true, hasContact = true,
      nearestWarriorTiles = Some(3.0), nearestVisibleArmedTiles = 30.0, behindAlphaTiles = 10.0),
    None)

  check("a visible gun off the contact line does not stop the catch-up",
    facts(enemyIsProtoss = true, enemyDragoonSeen = true),
    unit(isWarrior = true, shouldFight = true,
      nearestWarriorTiles = Some(3.0), nearestVisibleArmedTiles = 10.0, behindAlphaTiles = 10.0),
    Some(MarchLabel.CatchUp))

  check("a static gun on the path holds the catch-up",
    facts(enemyIsProtoss = true, enemyDragoonSeen = true),
    unit(isWarrior = true, shouldFight = true,
      nearestWarriorTiles = Some(3.0), nearestStaticTiles = Some(5.0), behindAlphaTiles = 10.0),
    None)

  check("a leader ahead of the alpha marches the legs",
    facts(enemyIsProtoss = true, enemyDragoonSeen = true),
    unit(isWarrior = true, shouldFight = true,
      nearestWarriorTiles = Some(3.0), nearestVisibleArmedTiles = 30.0, behindAlphaTiles = -10.0),
    None)

  check("the catch-up needs five tiles of trail",
    facts(enemyIsProtoss = true, enemyDragoonSeen = true),
    unit(isWarrior = true, shouldFight = true,
      nearestWarriorTiles = Some(3.0), nearestVisibleArmedTiles = 30.0, behindAlphaTiles = 3.0),
    None)

  test("the gate list order is explicit data, not ladder folklore") {
    assert(MarchGates.order == Seq(
      MarchLabel.DarkTemplarHunt,
      MarchLabel.CorsairHunt,
      MarchLabel.CatchUp,
      MarchLabel.MainMarch,
      MarchLabel.SuicideMission,
      MarchLabel.PairRide))
  }
}
