package Micro.Actions.Combat.Decisioning.Kiting

import org.scalatest.funsuite.AnyFunSuite

class ClusterSafetySuite extends AnyFunSuite {

  test("a hit inside the window means under fire") {
    assert(ClusterSafety.recentlyAttacked(0))
    assert(ClusterSafety.recentlyAttacked(3 * 24 - 1))
    assert( ! ClusterSafety.recentlyAttacked(3 * 24))
  }

  test("an attacked cluster stands one tile further") {
    assert(ClusterSafety.standoffGapPixels(200, 224, 1.8, clusterAttacked = true) ==
      ClusterSafety.standoffGapPixels(200, 224, 1.8, clusterAttacked = false) + 32)
  }

  test("already clear means no step") {
    assert(ClusterSafety.standoffGapPixels(400, 224, 1.8, clusterAttacked = true) == 0)
    assert(ClusterSafety.standoffGapPixels(400, 224, 1.8, clusterAttacked = false) == 0)
  }
}
