package Micro.Actions.Combat.Decisionmaking

import org.scalatest.funsuite.AnyFunSuite

class LurkerSafetySuite extends AnyFunSuite {

  test("policies split by worker and melee") {
    assert(LurkerSafety.LurkerPolicy.of(isWorker = true,  melee = false) == LurkerSafety.WorkerPolicy)
    assert(LurkerSafety.LurkerPolicy.of(isWorker = false, melee = true)  == LurkerSafety.ZealotPolicy)
    assert(LurkerSafety.LurkerPolicy.of(isWorker = false, melee = false) == LurkerSafety.RangedPolicy)
  }

  test("fresh melee keeps base plus melee bonus") {
    assert(LurkerSafety.ZealotPolicy.radiusBonusTiles(0.0) == 2.5)
  }

  test("wounds widen every policy linearly up to the full bonus") {
    assert(LurkerSafety.ZealotPolicy.radiusBonusTiles(1.0) == 5.0)
    assert(LurkerSafety.RangedPolicy.radiusBonusTiles(1.0) == 2.5)
    assert(LurkerSafety.WorkerPolicy.radiusBonusTiles(1.0) == 4.0)
    assert(LurkerSafety.ZealotPolicy.radiusBonusTiles(0.5) == 3.75)
  }

  test("melee flees burrowing without detectors, never with them") {
    assert(LurkerSafety.ZealotPolicy.fleesBurrowing(detectorsNear = false))
    assert( ! LurkerSafety.ZealotPolicy.fleesBurrowing(detectorsNear = true))
    assert( ! LurkerSafety.RangedPolicy.fleesBurrowing(detectorsNear = false))
  }

  test("bravery needs detectors, cannons, or (ranged only) army") {
    assert(LurkerSafety.ZealotPolicy.braveAgainstBurrowed(true, false, false))
    assert(LurkerSafety.ZealotPolicy.braveAgainstBurrowed(false, true, false))
    assert( ! LurkerSafety.ZealotPolicy.braveAgainstBurrowed(false, false, true))
    assert(LurkerSafety.RangedPolicy.braveAgainstBurrowed(false, false, true))
    assert( ! LurkerSafety.WorkerPolicy.braveAgainstBurrowed(true, true, true))
  }
}
