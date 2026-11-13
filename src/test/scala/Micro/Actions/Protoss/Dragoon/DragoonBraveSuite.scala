package Micro.Actions.Protoss.Dragoon

import org.scalatest.funsuite.AnyFunSuite

class DragoonBraveSuite extends AnyFunSuite {

  test("a ready shielded Dragoon stands up to Marines early") {
    assert(DragoonBrave.applies(dragoonVsTerran = true, shields = 10, cooldownLeft = 6, supplyUsed200 = 70, score = 0.0, foesMarines = true))
    assert(DragoonBrave.applies(dragoonVsTerran = true, shields = 80, cooldownLeft = 0, supplyUsed200 = 0, score = 0.0, foesMarines = true))
    assert(DragoonBrave.applies(dragoonVsTerran = true, shields = 80, cooldownLeft = 0, supplyUsed200 = 200, score = 400.0, foesMarines = true))
  }

  test("weak, cooling, late or mixed opposition still retreats") {
    assert( ! DragoonBrave.applies(dragoonVsTerran = true, shields = 9, cooldownLeft = 6, supplyUsed200 = 70, score = 0.0, foesMarines = true))
    assert( ! DragoonBrave.applies(dragoonVsTerran = true, shields = 10, cooldownLeft = 7, supplyUsed200 = 70, score = 0.0, foesMarines = true))
    assert( ! DragoonBrave.applies(dragoonVsTerran = true, shields = 10, cooldownLeft = 6, supplyUsed200 = 71, score = 399.9, foesMarines = true))
    assert( ! DragoonBrave.applies(dragoonVsTerran = true, shields = 10, cooldownLeft = 6, supplyUsed200 = 70, score = 0.0, foesMarines = false))
    assert( ! DragoonBrave.applies(dragoonVsTerran = false, shields = 80, cooldownLeft = 0, supplyUsed200 = 0, score = 0.0, foesMarines = true))
  }

  test("early game is low supply or a crushing score") {
    assert(DragoonBrave.earlyGame(supplyUsed200 = 70, score = 0.0))
    assert(DragoonBrave.earlyGame(supplyUsed200 = 200, score = 400.0))
    assert( ! DragoonBrave.earlyGame(supplyUsed200 = 71, score = 399.9))
  }
}
