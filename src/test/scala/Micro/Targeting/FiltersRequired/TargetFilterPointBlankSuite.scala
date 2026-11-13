package Micro.Targeting.FiltersRequired

import org.scalatest.funsuite.AnyFunSuite

class TargetFilterPointBlankSuite extends AnyFunSuite {

  test("a building at our feet is always legal, assigned or not") {
    assert(TargetFilterPointBlank.pointBlankLegal(targetIsBuilding = true, targetOutrangesUs = false, distPixels = 0.0, ownRangePixels = 16.0))
    assert(TargetFilterPointBlank.pointBlankLegal(targetIsBuilding = true, targetOutrangesUs = false, distPixels = 48.0, ownRangePixels = 16.0))
    assert( ! TargetFilterPointBlank.pointBlankLegal(targetIsBuilding = true, targetOutrangesUs = false, distPixels = 49.0, ownRangePixels = 16.0))
  }

  test("point-blank never forces a suicide into outranging statics") {
    assert( ! TargetFilterPointBlank.pointBlankLegal(targetIsBuilding = true, targetOutrangesUs = true, distPixels = 0.0, ownRangePixels = 16.0))
    assert( ! TargetFilterPointBlank.pointBlankLegal(targetIsBuilding = false, targetOutrangesUs = false, distPixels = 0.0, ownRangePixels = 16.0))
  }

  test("the point-blank reach is one tile past our own") {
    assert(TargetFilterPointBlank.pointBlankTiles == 1.0)
  }
}
