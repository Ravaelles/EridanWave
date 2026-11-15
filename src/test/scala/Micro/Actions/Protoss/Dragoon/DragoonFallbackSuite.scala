package Micro.Actions.Protoss.Dragoon

import Micro.Coordination.Pushing.TrafficPriorities
import org.scalatest.funsuite.AnyFunSuite

class DragoonFallbackSuite extends AnyFunSuite {

  test("a free Dragoon with a ranged target falls back from an all-rush pack") {
    assert(DragoonFallback.facesRush(
      isDragoon = true,
      pushPriority = TrafficPriorities.Freedom,
      hasRangedTarget = true,
      allRushers = true))
  }

  test("no claim without the hull, the target, the rush or the freedom") {
    assert( ! DragoonFallback.facesRush(false, TrafficPriorities.Freedom, true, true))
    assert( ! DragoonFallback.facesRush(true, TrafficPriorities.Shove, true, true))
    assert( ! DragoonFallback.facesRush(true, TrafficPriorities.Freedom, false, true))
    assert( ! DragoonFallback.facesRush(true, TrafficPriorities.Freedom, true, false))
  }
}
