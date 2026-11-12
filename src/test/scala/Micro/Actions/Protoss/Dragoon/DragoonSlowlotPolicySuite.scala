package Micro.Actions.Protoss.Dragoon

import Micro.Coordination.Pushing.TrafficPriorities
import org.scalatest.funsuite.AnyFunSuite

class DragoonSlowlotPolicySuite extends AnyFunSuite {

  test("shots of slack scale with hit points") {
    assert(DragoonSlowlotPolicy.hitsLeft(0) == 0)
    assert(DragoonSlowlotPolicy.hitsLeft(13) == 0)
    assert(DragoonSlowlotPolicy.hitsLeft(14) == 1)
    assert(DragoonSlowlotPolicy.hitsLeft(80) == 5)
  }

  test("safety adds tiles of margin to shots of slack") {
    assert(DragoonSlowlotPolicy.safety(marginPixels = 64, totalHealth = 14) == 2 + 1)
    assert(DragoonSlowlotPolicy.safety(marginPixels = 0, totalHealth = 0) == 0)
  }

  test("dying Dragoons shove, comfortable ones pardon") {
    assert(DragoonSlowlotPolicy.priorityForSafety(0) == TrafficPriorities.Shove)
    assert(DragoonSlowlotPolicy.priorityForSafety(2) == TrafficPriorities.Shove)
    assert(DragoonSlowlotPolicy.priorityForSafety(3) == TrafficPriorities.Bump)
    assert(DragoonSlowlotPolicy.priorityForSafety(4) == TrafficPriorities.Bump)
    assert(DragoonSlowlotPolicy.priorityForSafety(5) == TrafficPriorities.Nudge)
    assert(DragoonSlowlotPolicy.priorityForSafety(6) == TrafficPriorities.Nudge)
    assert(DragoonSlowlotPolicy.priorityForSafety(7) == TrafficPriorities.Pardon)
  }

  test("more slack buys a closer stand, never closer than one tile") {
    assert(DragoonSlowlotPolicy.marginRequiredPixels(0) == 128)
    assert(DragoonSlowlotPolicy.marginRequiredPixels(14) == 120)
    assert(DragoonSlowlotPolicy.marginRequiredPixels(140) == 48)
    assert(DragoonSlowlotPolicy.marginRequiredPixels(1000) == 32)
  }

  test("positive delta holds, negative delta runs") {
    assert(DragoonSlowlotPolicy.marginDeltaPixels(128, 0) == 0)
    assert(DragoonSlowlotPolicy.marginDeltaPixels(200, 0) > 0)
    assert(DragoonSlowlotPolicy.marginDeltaPixels(0, 0) < 0)
  }
}
