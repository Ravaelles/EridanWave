package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class DragoonMineDefuseSuite extends AnyFunSuite {

  test("detected mines inside ten tiles get shot") {
    assert(DragoonMineDefuse.inDefuseRange(0.0))
    assert(DragoonMineDefuse.inDefuseRange(10.0))
    assert( ! DragoonMineDefuse.inDefuseRange(10.01))
  }

  test("any answering enemy inside twelve tiles holds the defuse") {
    assert(DragoonMineDefuse.safeToDefuse(Double.PositiveInfinity))
    assert(DragoonMineDefuse.safeToDefuse(12.01))
    assert( ! DragoonMineDefuse.safeToDefuse(12.0))
    assert( ! DragoonMineDefuse.safeToDefuse(0.0))
  }
}
