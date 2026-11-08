package Lifecycle

import org.scalatest.funsuite.AnyFunSuite

class GameSummarySuite extends AnyFunSuite {

  test("victory box matches the Atlantis layout") {
    assert(GameSummary.box(isWinner = true, killed = 3, lost = 21) ==
      "#####################################\n" +
      "############ VICTORY! ###############\n" +
      "############ Lost: 21 ################\n" +
      "########## Killed: 3 ################\n" +
      "#####################################")
  }

  test("defeat box matches the Atlantis layout") {
    assert(GameSummary.box(isWinner = false, killed = 3, lost = 21).contains("############ Defeat... ###############"))
  }

  test("details report time, units and signed resource balance") {
    assert(GameSummary.details(frames = 2400, killed = 8, lost = 2, killedResources = 1200, lostResources = 400) ==
      "### Total time: 100 seconds. ###\n" +
      "### Units killed/lost: 8/2 ###\n" +
      "### Resource killed/lost: +800 ###")
    assert(GameSummary.details(frames = 24, killed = 0, lost = 1, killedResources = 0, lostResources = 50).contains(
      "### Resource killed/lost: -50 ###"))
  }
}
