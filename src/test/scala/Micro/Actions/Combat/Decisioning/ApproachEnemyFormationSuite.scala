package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class ApproachEnemyFormationSuite extends AnyFunSuite {

  test("the formation dresses at fifteen against siege tanks, twelve otherwise") {
    assert(ApproachEnemyFormation.formationRangeTiles(terranSieged = true) == 15.0)
    assert(ApproachEnemyFormation.formationRangeTiles(terranSieged = false) == 12.0)
  }

  test("the dress holds past its edge and the line steps together") {
    assert(ApproachEnemyFormation.releaseMarginTiles == 1.5)
    assert(ApproachEnemyFormation.advanceEveryFrames == 20)
    assert(ApproachEnemyFormation.advanceWindowFrames == 8)
    assert(ApproachEnemyFormation.advanceStepTiles == 0.7)
  }

  test("the pack brawls together: one fighting blade releases the dress") {
    assert(ApproachEnemyFormation.packContactTiles == 2.0)
    assert(ApproachEnemyFormation.packReleaseTiles == 3.5)
    // Each of the four fight signs alone releases the dress.
    assert(ApproachEnemyFormation.packMateFights(fired = true, engagedUpon = false, hasHittable = false, hasThreatInRange = false, nearestFoeTiles = 99.0))
    assert(ApproachEnemyFormation.packMateFights(fired = false, engagedUpon = true, hasHittable = false, hasThreatInRange = false, nearestFoeTiles = 99.0))
    assert(ApproachEnemyFormation.packMateFights(fired = false, engagedUpon = false, hasHittable = true, hasThreatInRange = false, nearestFoeTiles = 99.0))
    assert(ApproachEnemyFormation.packMateFights(fired = false, engagedUpon = false, hasHittable = false, hasThreatInRange = true, nearestFoeTiles = 99.0))
    assert(ApproachEnemyFormation.packMateFights(fired = false, engagedUpon = false, hasHittable = false, hasThreatInRange = false, nearestFoeTiles = 2.0))
    // Clear on every sign and past the release band the dress resumes.
    assert( ! ApproachEnemyFormation.packMateFights(fired = false, engagedUpon = false, hasHittable = false, hasThreatInRange = false, nearestFoeTiles = 2.01))
    assert(ApproachEnemyFormation.packClear(fired = false, engagedUpon = false, hasHittable = false, hasThreatInRange = false, nearestFoeTiles = 3.51))
    assert( ! ApproachEnemyFormation.packClear(fired = false, engagedUpon = false, hasHittable = false, hasThreatInRange = false, nearestFoeTiles = 3.5))
    assert( ! ApproachEnemyFormation.packClear(fired = true, engagedUpon = false, hasHittable = false, hasThreatInRange = false, nearestFoeTiles = 99.0))
  }
}
