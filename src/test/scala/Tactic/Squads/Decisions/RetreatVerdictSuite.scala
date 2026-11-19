package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class RetreatVerdictSuite extends AnyFunSuite {

  private def hold(
    isWarrior: Boolean = true,
    isReaver: Boolean = false,
    canFlee: Boolean = true,
    stormHold: Boolean = false,
    shouldFight: Boolean = false,
    totalHp: Int = 200,
    unitsScore: Double = 100.0,
    nearestArmedTiles: Double = 40.0,
    proxyRush: Boolean = false,
    hardCounterKnown: Boolean = false,
    nearestFoeWarriorTiles: Double = 0.0): Boolean =
    RetreatVerdict.shouldHold(
      isWarrior,
      isReaver,
      canFlee,
      stormHold,
      shouldFight,
      totalHp,
      unitsScore,
      nearestArmedTiles,
      proxyRush,
      hardCounterKnown,
      nearestFoeWarriorTiles)

  test("retreat labels carry the reason plus both skim shares in tens") {
    assert(RetreatVerdict.retreatLabel("OutOfContact", 80.0, 20.0) == "OutOfContact 80vs20")
    assert(RetreatVerdict.retreatLabel("OutmatchedGoon", 185.0, 100.0) == "OutmatchedGoon 60vs40")
    assert(RetreatVerdict.retreatLabel("Sim", 0.0, 0.0) == "Sim")
    assert(RetreatVerdict.retreatLabel("", 80.0, 20.0) == "80vs20")
  }

  test("a beaten warrior with room to run holds the retreat") {
    assert(hold(nearestArmedTiles = 5.0))
  }

  test("winning the verdict releases the retreat") {
    assert( ! hold(shouldFight = true))
  }

  test("non-warriors never hold a warrior retreat") {
    assert( ! hold(isWarrior = false))
  }

  test("reavers keep their own brain") {
    assert( ! hold(isReaver = true))
  }

  test("a committed hull holds no retreat (sides with the technique arm)") {
    assert( ! hold(canFlee = false))
  }

  test("a casting templar holds its storm instead") {
    assert( ! hold(stormHold = true))
  }

  test("retreat permission still gates the verdict") {
    assert( ! hold(unitsScore = 866.0, proxyRush = false))
    assert(hold(unitsScore = 866.0, nearestArmedTiles = 3.0, hardCounterKnown = true))
    assert( ! hold(unitsScore = 866.0, nearestArmedTiles = 3.0))
    assert( ! hold(unitsScore = 238.0, nearestArmedTiles = 3.0))
    assert( ! hold(unitsScore = 100.0, proxyRush = true))
  }

  test("a dying hull always keeps its way out") {
    assert(hold(totalHp = 35, unitsScore = 866.0, nearestArmedTiles = 40.0, proxyRush = true))
    assert( ! hold(totalHp = 36, unitsScore = 866.0, nearestArmedTiles = 40.0, proxyRush = true))
  }

  test("an unknown unit holds no retreat") {
    RetreatVerdict.reset()
    assert( ! RetreatVerdict.held(id = 7, frame = 100))
  }

  test("the shared bus enters sharp and releases past the hold") {
    RetreatVerdict.reset()
    RetreatVerdict.update(id = 7, rawHold = true, frame = 100)
    assert(RetreatVerdict.held(id = 7, frame = 100))
    RetreatVerdict.update(id = 7, rawHold = false, frame = 110)
    assert(RetreatVerdict.held(id = 7, frame = 110))
    assert(RetreatVerdict.held(id = 7, frame = 110 + RetreatVerdict.holdFrames - 1))
    assert( ! RetreatVerdict.held(id = 7, frame = 110 + RetreatVerdict.holdFrames))
  }

  test("re-entering re-arms the window") {
    RetreatVerdict.reset()
    RetreatVerdict.update(id = 7, rawHold = true, frame = 100)
    RetreatVerdict.update(id = 7, rawHold = false, frame = 110)
    RetreatVerdict.update(id = 7, rawHold = true, frame = 115)
    assert(RetreatVerdict.held(id = 7, frame = 200))
  }

  test("units do not share a bus") {
    RetreatVerdict.reset()
    RetreatVerdict.update(id = 7, rawHold = true, frame = 100)
    assert( ! RetreatVerdict.held(id = 8, frame = 100))
  }

  test("reset clears the bus for the next game") {
    RetreatVerdict.update(id = 7, rawHold = true, frame = 100)
    RetreatVerdict.reset()
    assert( ! RetreatVerdict.held(id = 7, frame = 100))
  }

  test("no enemy warriors inside twelve tiles means no retreat, ever") {
    assert(RetreatVerdict.lonely(12.01))
    assert( ! RetreatVerdict.lonely(12.0))
    assert( ! RetreatVerdict.lonely(0.0))
    assert(hold(nearestFoeWarriorTiles = 12.0, nearestArmedTiles = 3.0))
    assert( ! hold(nearestFoeWarriorTiles = 12.01))
    assert( ! hold(nearestFoeWarriorTiles = 100.0))
    assert(hold(nearestFoeWarriorTiles = 11.9, nearestArmedTiles = 3.0))
  }
}
