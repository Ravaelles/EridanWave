package Micro.Actions.Protoss.Reaver

import org.scalatest.funsuite.AnyFunSuite

class ReaverFireDisciplineSuite extends AnyFunSuite {

  test("a ready Reaver with an enemy nearby must shoot first") {
    assert(ReaverFireDiscipline.mustShootFirst(shotReady = true, enemyNear = true))
  }

  test("no enemy means nothing to hold for") {
    assert( ! ReaverFireDiscipline.mustShootFirst(shotReady = true, enemyNear = false))
  }

  test("a cooling Reaver boards freely") {
    assert( ! ReaverFireDiscipline.mustShootFirst(shotReady = false, enemyNear = true))
    assert( ! ReaverFireDiscipline.mustShootFirst(shotReady = false, enemyNear = false))
  }

  test("a shot is ready only with a loaded Scarab, none in flight, off cooldown") {
    assert(ReaverFireDiscipline.shotReady(scarabs = 1, hasActiveScarab = false, cooling = false))
    assert( ! ReaverFireDiscipline.shotReady(scarabs = 0, hasActiveScarab = false, cooling = false))
    assert( ! ReaverFireDiscipline.shotReady(scarabs = 1, hasActiveScarab = true, cooling = false))
    assert( ! ReaverFireDiscipline.shotReady(scarabs = 1, hasActiveScarab = false, cooling = true))
  }
}
