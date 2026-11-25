package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class ClusterCohesionSuite extends AnyFunSuite {

  test("drift band stays fourteen tiles") {
    assert(ClusterCohesion.maxDriftTiles == 14.0)
  }

  test("operate band ends eight tiles out, releases a tile inside") {
    assert(ClusterCohesion.maxOperateTiles == 8.0)
    assert(ClusterCohesion.operateReleaseTiles == 7.0)
    assert(ClusterCohesion.operateReleaseTiles < ClusterCohesion.maxOperateTiles)
  }

  private def engaged(distTiles: Double): Boolean =
    ClusterCohesion.mustRejoin(
      isWarrior = true,
      retreating = false,
      coolingUnderFire = false,
      inBattle = true,
      macroGame = true,
      hasLocalContact = false,
      verdictDuel = false,
      distTiles = distTiles)

  test("past eight tiles the hull comes home, victim or not") {
    assert(engaged(8.1))
    assert(engaged(20.0))
    assert( ! engaged(8.0))
  }

  test("the forced return keeps every other gate of the drift band") {
    val base = engaged(20.0)
    assert(base)
    assert( ! ClusterCohesion.mustRejoin(false, false, false, true, true, false, false, 20.0))
    assert( ! ClusterCohesion.mustRejoin(true, true, false, true, true, false, false, 20.0))
    assert( ! ClusterCohesion.mustRejoin(true, false, true, true, true, false, false, 20.0))
    assert( ! ClusterCohesion.mustRejoin(true, false, false, false, true, false, false, 20.0))
    assert( ! ClusterCohesion.mustRejoin(true, false, false, true, false, false, false, 20.0))
    assert( ! ClusterCohesion.mustRejoin(true, false, false, true, true, true, false, 20.0))
    assert( ! ClusterCohesion.mustRejoin(true, false, false, true, true, false, true, 20.0))
  }
}
