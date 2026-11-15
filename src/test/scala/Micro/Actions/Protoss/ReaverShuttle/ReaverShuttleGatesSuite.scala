package Micro.Actions.Protoss.ReaverShuttle

import Tactic.Squads.Squad
import org.mockito.Mockito
import org.scalatest.funsuite.AnyFunSuite

class ReaverShuttleGatesSuite extends AnyFunSuite {

  test("short hops walk, long hauls ride") {
    assert( ! ReaverShuttleFerry.needsRide(0.0))
    assert( ! ReaverShuttleFerry.needsRide(3.0))
    assert(ReaverShuttleFerry.needsRide(3.01))
    assert(ReaverShuttleFerry.needsRide(30.0))
  }

  test("the squadless ride still sees its mate through restaffing") {
    val squad = Mockito.mock(classOf[Squad])
    assert(ReaverShuttlePair.mateInSquad(None, None))
    assert(ReaverShuttlePair.mateInSquad(None, Some(squad)))
    assert(ReaverShuttlePair.mateInSquad(Some(squad), Some(squad)))
    assert( ! ReaverShuttlePair.mateInSquad(Some(squad), None))
    assert( ! ReaverShuttlePair.mateInSquad(Some(squad), Some(Mockito.mock(classOf[Squad]))))
  }

  test("the ride stays with an unloaded mate operating close") {
    assert(ReaverShuttlePair.shouldEscort(0.0))
    assert(ReaverShuttlePair.shouldEscort(15.0))
    assert( ! ReaverShuttlePair.shouldEscort(15.01))
  }

  test("the escort latch enters sharp and releases two tiles past escort range") {
    ReaverShuttlePair.reset()
    assert(ReaverShuttlePair.escortLatched(1, 0.0))
    assert(ReaverShuttlePair.escortLatched(1, 14.9))
    assert(ReaverShuttlePair.escortLatched(1, 16.9))
    assert( ! ReaverShuttlePair.escortLatched(1, 17.01))
    assert( ! ReaverShuttlePair.escortLatched(1, 16.0))
    assert(ReaverShuttlePair.escortLatched(1, 14.9))
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
    assert(ReaverShuttleReboard.mayReboard(threatDistTiles = 0.1, wound01 = 0.0))
    assert( ! ReaverShuttleReboard.mayReboard(threatDistTiles = 2.0, wound01 = 0.0))
    assert(ReaverShuttleReboard.mayReboard(threatDistTiles = 7.99, wound01 = 1.0))
    assert( ! ReaverShuttleReboard.mayReboard(threatDistTiles = 8.0, wound01 = 1.0))
    assert(ReaverShuttleReboard.mayReboard(threatDistTiles = 4.99, wound01 = 0.5))
    assert( ! ReaverShuttleReboard.mayReboard(threatDistTiles = 5.0, wound01 = 0.5))
  }

  test("a Reaver inside the reach of an outranged static backs off to max reach") {    assert(ReaverShuttleStaticAttack.advantageBackoff(ourRangeTiles = 8.0, staticRangeTiles = 7.0, distTiles = 6.17))
    assert(ReaverShuttleStaticAttack.advantageBackoff(ourRangeTiles = 8.0, staticRangeTiles = 7.0, distTiles = 7.0))
    assert( ! ReaverShuttleStaticAttack.advantageBackoff(ourRangeTiles = 8.0, staticRangeTiles = 7.0, distTiles = 7.5))
    assert( ! ReaverShuttleStaticAttack.advantageBackoff(ourRangeTiles = 8.0, staticRangeTiles = 7.0, distTiles = 8.0))
    assert( ! ReaverShuttleStaticAttack.advantageBackoff(ourRangeTiles = 7.0, staticRangeTiles = 8.0, distTiles = 6.0))
    assert( ! ReaverShuttleStaticAttack.advantageBackoff(ourRangeTiles = 8.0, staticRangeTiles = 8.0, distTiles = 7.0))
  }

  test("a pressured mid-shot boards while its Scarab is still flying") {
    def rescue(loaded: Boolean = false, committed: Boolean = false, flying: Boolean = true, cooldown: Int = 0, latency: Int = 3, threatened: Boolean = true): Boolean =
      ReaverShuttleReboard.rescueScarab(loaded, committed, flying, cooldown, latency, threatened)
    assert(rescue())
    assert( ! rescue(loaded = true))
    assert( ! rescue(committed = true))
    assert( ! rescue(flying = false))
    assert( ! rescue(cooldown = 4, latency = 3))
    assert(rescue(cooldown = 3, latency = 3))
    assert( ! rescue(threatened = false))
  }
}
