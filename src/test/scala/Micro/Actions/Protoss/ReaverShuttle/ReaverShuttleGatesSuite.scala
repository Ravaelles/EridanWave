package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class ReaverShuttleGatesSuite extends AnyFunSuite {

  test("short hops walk, long hauls ride") {
    assert( ! ReaverShuttleFerry.needsRide(0.0))
    assert( ! ReaverShuttleFerry.needsRide(3.0))
    assert(ReaverShuttleFerry.needsRide(3.01))
    assert(ReaverShuttleFerry.needsRide(30.0))
  }

  test("a threatened landing aborts the static attack") {
    assert(ReaverShuttleStaticAttack.shouldLand(distTiles = 11.9, attackedAtLanding = false, threatDistTiles = 99.0, wound01 = 0.0))
    assert( ! ReaverShuttleStaticAttack.shouldLand(distTiles = 12.0, attackedAtLanding = false, threatDistTiles = 99.0, wound01 = 0.0))
    assert( ! ReaverShuttleStaticAttack.shouldLand(distTiles = 5.0, attackedAtLanding = true, threatDistTiles = 99.0, wound01 = 0.0))
    assert( ! ReaverShuttleStaticAttack.shouldLand(distTiles = 5.0, attackedAtLanding = false, threatDistTiles = 4.0, wound01 = 0.0))
    assert(ReaverShuttleStaticAttack.shouldLand(distTiles = 5.0, attackedAtLanding = false, threatDistTiles = 4.01, wound01 = 0.0))
  }

  test("the safety margin grows with wounds") {
    assert(ReaverShuttleStaticAttack.marginTiles(0.0) == 4.0)
    assert(ReaverShuttleStaticAttack.marginTiles(1.0) == 10.0)
    assert( ! ReaverShuttleStaticAttack.shouldLand(distTiles = 5.0, attackedAtLanding = false, threatDistTiles = 9.0, wound01 = 1.0))
    assert(ReaverShuttleStaticAttack.shouldLand(distTiles = 5.0, attackedAtLanding = false, threatDistTiles = 10.01, wound01 = 1.0))
  }

  test("a grounded Reaver reboards only under direct threat") {
    assert( ! ReaverShuttleReboard.mayReboard(threatDistTiles = 0.1, wound01 = 0.0))
    assert(ReaverShuttleReboard.mayReboard(threatDistTiles = 7.99, wound01 = 1.0))
    assert( ! ReaverShuttleReboard.mayReboard(threatDistTiles = 8.0, wound01 = 1.0))
    assert(ReaverShuttleReboard.mayReboard(threatDistTiles = 3.99, wound01 = 0.5))
    assert( ! ReaverShuttleReboard.mayReboard(threatDistTiles = 4.0, wound01 = 0.5))
  }
}
