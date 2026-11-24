package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class ApproachBuildingFormationSuite extends AnyFunSuite {

  test("the formation dresses at fifteen against siege tanks, twelve otherwise") {
    assert(ApproachBuildingFormation.formationRangeTiles(terranSieged = true) == 15.0)
    assert(ApproachBuildingFormation.formationRangeTiles(terranSieged = false) == 12.0)
  }

  test("the dress holds past its edge and the line steps together") {
    assert(ApproachBuildingFormation.releaseMarginTiles == 1.5)
    assert(ApproachBuildingFormation.advanceEveryFrames == 20)
    assert(ApproachBuildingFormation.advanceWindowFrames == 8)
    assert(ApproachBuildingFormation.advanceStepTiles == 0.25)
  }

  test("the pack brawls together: one fighting blade releases the dress") {
    assert(ApproachBuildingFormation.packContactTiles == 2.0)
    assert(ApproachBuildingFormation.packReleaseTiles == 3.5)
    // Each of the four fight signs alone releases the dress.
    assert(ApproachBuildingFormation.packMateFights(fired = true, engagedUpon = false, hasHittable = false, hasThreatInRange = false, nearestFoeTiles = 99.0))
    assert(ApproachBuildingFormation.packMateFights(fired = false, engagedUpon = true, hasHittable = false, hasThreatInRange = false, nearestFoeTiles = 99.0))
    assert(ApproachBuildingFormation.packMateFights(fired = false, engagedUpon = false, hasHittable = true, hasThreatInRange = false, nearestFoeTiles = 99.0))
    assert(ApproachBuildingFormation.packMateFights(fired = false, engagedUpon = false, hasHittable = false, hasThreatInRange = true, nearestFoeTiles = 99.0))
    assert(ApproachBuildingFormation.packMateFights(fired = false, engagedUpon = false, hasHittable = false, hasThreatInRange = false, nearestFoeTiles = 2.0))
    // Clear on every sign and past the release band the dress resumes.
    assert( ! ApproachBuildingFormation.packMateFights(fired = false, engagedUpon = false, hasHittable = false, hasThreatInRange = false, nearestFoeTiles = 2.01))
    assert(ApproachBuildingFormation.packClear(fired = false, engagedUpon = false, hasHittable = false, hasThreatInRange = false, nearestFoeTiles = 3.51))
    assert( ! ApproachBuildingFormation.packClear(fired = false, engagedUpon = false, hasHittable = false, hasThreatInRange = false, nearestFoeTiles = 3.5))
    assert( ! ApproachBuildingFormation.packClear(fired = true, engagedUpon = false, hasHittable = false, hasThreatInRange = false, nearestFoeTiles = 99.0))
  }
}
