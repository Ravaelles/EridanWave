package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class PairGlueSuite extends AnyFunSuite {

  test("glue holds an on-foot mate inside the band") {
    assert(PairGlue.inGlueBand(mateOnFoot = true, mateDistTiles = 0.0))
    assert(PairGlue.inGlueBand(mateOnFoot = true, mateDistTiles = 15.0))
    assert( ! PairGlue.inGlueBand(mateOnFoot = true, mateDistTiles = 15.1))
    assert( ! PairGlue.inGlueBand(mateOnFoot = false, mateDistTiles = 1.0))
  }

  test("a pressured ride retreats to its mate, never away") {
    assert(PairGlue.fleeToMate(mateOnFoot = true, mateDistTiles = 5.0))
    assert( ! PairGlue.fleeToMate(mateOnFoot = true, mateDistTiles = 30.0))
    assert( ! PairGlue.fleeToMate(mateOnFoot = false, mateDistTiles = 1.0))
  }

  test("a safe hover on the hull never moves") {
    assert(PairGlue.capHover(0.0, 0.0, 0.0, 0.0) == ((0.0, 0.0)))
    assert(PairGlue.capHover(0.0, 0.0, 0.3, 0.4) == ((0.3, 0.4)))
    assert(PairGlue.capHover(0.0, 0.0, 1.0, 0.0) == ((1.0, 0.0)))
  }

  test("a far hover pulls back to board reach") {
    val (x, y) = PairGlue.capHover(0.0, 0.0, 6.0, 8.0)
    assert(Math.abs(Math.sqrt(x * x + y * y) - 2.0) < 1e-9)
    assert(Math.abs(x - 1.2) < 1e-9)
    assert(Math.abs(y - 1.6) < 1e-9)
  }
}
