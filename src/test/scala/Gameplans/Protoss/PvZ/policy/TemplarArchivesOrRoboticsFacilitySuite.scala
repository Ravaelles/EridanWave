package Gameplans.Protoss.PvZ.policy

import org.scalatest.funsuite.AnyFunSuite

class TemplarArchivesOrRoboticsFacilitySuite extends AnyFunSuite {

  private def prioritize(
    haveArchives : Boolean = true,
    hasRobo      : Boolean = false,
    dtProduced   : Int     = 0,
    gas          : Int     = 0,
    lurkersShown : Boolean = false,
    lurkersLikely: Boolean = false): Boolean =
    TemplarArchivesOrRoboticsFacility.shouldPrioritizeTemplarsOverFirstRoboticsFacility(
      haveArchives, hasRobo, dtProduced, gas, lurkersShown, lurkersLikely)

  test("a fresh Archives with no Templars built holds the first Robo") {
    assert(prioritize())
    assert(prioritize(dtProduced = 1, gas = 259))
  }

  test("two lifetime Templars release the Robo") {
    assert( ! prioritize(dtProduced = 2))
    assert( ! prioritize(dtProduced = 9))
  }

  test("a flush bank releases the Robo") {
    assert( ! prioritize(gas = 260))
    assert( ! prioritize(dtProduced = 1, gas = 1000))
  }

  test("shown or likely Lurkers skip the Templar detour") {
    assert( ! prioritize(lurkersShown = true))
    assert( ! prioritize(lurkersLikely = true))
    assert( ! prioritize(dtProduced = 1, lurkersShown = true))
  }

  test("no Archives or an existing Robo means nothing to prioritize") {
    assert( ! prioritize(haveArchives = false))
    assert( ! prioritize(hasRobo = true))
    assert( ! prioritize(haveArchives = false, hasRobo = true, dtProduced = 9, gas = 9999, lurkersShown = true, lurkersLikely = true))
  }
}
