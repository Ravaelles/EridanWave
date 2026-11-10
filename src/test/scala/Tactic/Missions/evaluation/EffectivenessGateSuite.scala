package Tactic.Missions.evaluation

import org.scalatest.funsuite.AnyFunSuite
import Tactic.Missions.evaluation.EffectivenessGate.EffectivenessParams

class EffectivenessGateSuite extends AnyFunSuite {

  private val params = EffectivenessParams(minAlways = 2, maxAlive = 7, recentWindow = 2, gasFlood = 400)

  private def allows(produced: Int, alive: Int, recent: Double, average: Double, gas: Int): Boolean =
    EffectivenessGate.allows(produced, alive, recent, average, gas, params)

  test("the required minimum always builds") {
    assert(allows(produced = 0, alive = 0, recent = 0.0, average = 0.0, gas = 0))
    assert(allows(produced = 1, alive = 0, recent = -500.0, average = -500.0, gas = 0))
  }

  test("the alive cap vetoes everything") {
    assert( ! allows(produced = 2, alive = 7, recent = 999.0, average = 999.0, gas = 9999))
  }

  test("gas flood builds while lifetime breaks even") {
    assert(allows(produced = 3, alive = 3, recent = -100.0, average = 0.0, gas = 400))
    assert(allows(produced = 3, alive = 3, recent = -100.0, average = 50.0, gas = 800))
  }

  test("gas flood refuses a lifetime loss") {
    assert( ! allows(produced = 3, alive = 3, recent = -100.0, average = -0.5, gas = 9999))
  }

  test("a profitable recent window builds without flood") {
    assert(allows(produced = 2, alive = 2, recent = 0.5, average = -100.0, gas = 0))
  }

  test("a dead recent window with dead average stands down") {
    assert( ! allows(produced = 2, alive = 0, recent = -200.0, average = -200.0, gas = 0))
    assert( ! allows(produced = 4, alive = 1, recent = 0.0, average = 100.0, gas = 399))
  }
}
