package Tactic.Missions.drops

import org.scalatest.funsuite.AnyFunSuite

class StormDropEnergySuite extends AnyFunSuite {

  test("spent Templars go home") {
    assert(StormDropEnergy.drained(Seq(10, 74)))
  }

  test("empty crew counts as spent") {
    assert(StormDropEnergy.drained(Seq.empty))
  }

  test("one charged Templar stays") {
    assert( ! StormDropEnergy.drained(Seq(10, 75)))
  }
}
