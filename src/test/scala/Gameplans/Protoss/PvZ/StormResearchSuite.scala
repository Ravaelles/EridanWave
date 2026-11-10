package Gameplans.Protoss.PvZ

import org.scalatest.funsuite.AnyFunSuite

class StormResearchSuite extends AnyFunSuite {

  test("one lifetime Templar never commits") {
    assert( ! StormResearch.shouldResearch(produced = 1, complete = 1, extant = 2, gas = 9999))
    assert( ! StormResearch.shouldResearch(produced = 0, complete = 0, extant = 0, gas = 0))
  }

  test("no live caster blocks research") {
    assert( ! StormResearch.shouldResearch(produced = 4, complete = 0, extant = 0, gas = 9999))
    assert( ! StormResearch.shouldResearch(produced = 2, complete = 0, extant = 1, gas = 9999))
  }

  test("bank excuses a lone caster, standing pair excuses a thin bank") {
    assert(StormResearch.shouldResearch(produced = 2, complete = 1, extant = 1, gas = 350))
    assert(StormResearch.shouldResearch(produced = 2, complete = 1, extant = 2, gas = 0))
    assert( ! StormResearch.shouldResearch(produced = 2, complete = 1, extant = 1, gas = 349))
  }

  test("thirteen idle Templars with banked gas research") {
    assert(StormResearch.shouldResearch(produced = 13, complete = 13, extant = 13, gas = 500))
  }
}
