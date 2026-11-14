package Macro.Actions

import org.scalatest.funsuite.AnyFunSuite

class ZealotPumpSuite extends AnyFunSuite {

  test("small lines keep pumping Zealots") {
    assert( ! ZealotPump.shouldStop(zealots = 15, dragoons = 0))
    assert( ! ZealotPump.shouldStop(zealots = 0, dragoons = 0))
  }

  test("a swarm ten times the Dragoon count stops the gates") {
    assert(ZealotPump.shouldStop(zealots = 16, dragoons = 0))
    assert(ZealotPump.shouldStop(zealots = 100, dragoons = 10))
    assert( ! ZealotPump.shouldStop(zealots = 100, dragoons = 11))
  }
}
