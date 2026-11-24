package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class ApproachBuildingFormationSuite extends AnyFunSuite {

  test("the formation dresses at fifteen against siege tanks, twelve otherwise") {
    assert(ApproachBuildingFormation.formationRangeTiles(terranSieged = true) == 15.0)
    assert(ApproachBuildingFormation.formationRangeTiles(terranSieged = false) == 12.0)
  }
}
