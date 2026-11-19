package Debugging

import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

class DeathWatchSuite extends AnyFunSuite {

  test("the units we cannot afford to lose blindly are watched") {
    assert(DeathWatch.watched(Protoss.Observer))
    assert(DeathWatch.watched(Protoss.Shuttle))
    assert(DeathWatch.watched(Protoss.DarkTemplar))
    assert(DeathWatch.watched(Protoss.Reaver))
    assert( ! DeathWatch.watched(Protoss.Dragoon))
  }

  test("ums micro maps watch every warrior, ladder keeps the quiet set") {
    assert(DeathWatch.watches(Protoss.Observer, umsMode = false))
    assert(DeathWatch.watches(Protoss.Dragoon, umsMode = true))
    assert(DeathWatch.watches(Protoss.Zealot, umsMode = true))
    assert( ! DeathWatch.watches(Protoss.Dragoon, umsMode = false))
    assert( ! DeathWatch.watches(Protoss.Pylon, umsMode = true))
  }
}
