package Micro.Actions.Protoss.DarkTemplar

import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarStaticPushSuite extends AnyFunSuite {

  test("pins the push margin") {
    assert(DarkTemplarStaticPush.attackMarginTiles == 0.1)
  }

  test("anything hittable in range plus the margin is swiped") {
    assert(DarkTemplarStaticPush.inPushRange(distPixels = 0.0, rangePixels = 32.0))
    assert(DarkTemplarStaticPush.inPushRange(distPixels = 32.0, rangePixels = 32.0))
    assert(DarkTemplarStaticPush.inPushRange(distPixels = 35.2, rangePixels = 32.0))
    assert( ! DarkTemplarStaticPush.inPushRange(distPixels = 35.3, rangePixels = 32.0))
    assert( ! DarkTemplarStaticPush.inPushRange(distPixels = 64.0, rangePixels = 32.0))
  }
}
