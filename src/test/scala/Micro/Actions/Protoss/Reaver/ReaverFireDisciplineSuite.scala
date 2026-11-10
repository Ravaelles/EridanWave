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
}
