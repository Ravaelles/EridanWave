package Micro.Actions.Protoss

import org.scalatest.funsuite.AnyFunSuite

class ReaverRefreshCooldownSuite extends AnyFunSuite {

  private val idle = ReaverSituation()

  test("a cooling Reaver refreshes under pressure") {
    assert(ReaverRefreshCooldown.applies(idle.copy(cooling = true, threatened = true)))
  }

  test("a cooling Reaver without pressure stands ready") {
    assert( ! ReaverRefreshCooldown.applies(idle.copy(cooling = true)))
  }

  test("a loaded Reaver never refreshes") {
    assert( ! ReaverRefreshCooldown.applies(idle.copy(loaded = true, cooling = true, threatened = true)))
  }
}
