package Micro.Actions.Protoss.Dragoon

import Micro.Coordination.Pushing.TrafficPriorities
import org.scalatest.funsuite.AnyFunSuite

class DragoonFallbackSuite extends AnyFunSuite {

  test("a free Dragoon with a ranged target falls back from an all-rush pack with ranged backup") {
    assert(DragoonFallback.facesRush(
      isDragoon = true,
      pushPriority = TrafficPriorities.Freedom,
      hasRangedTarget = true,
      allRushers = true,
      enemyHasRanged = true))
  }

  test("no claim without the hull, the target, the rush, the freedom or the ranged backup") {
    assert( ! DragoonFallback.facesRush(false, TrafficPriorities.Freedom, true, true, true))
    assert( ! DragoonFallback.facesRush(true, TrafficPriorities.Shove, true, true, true))
    assert( ! DragoonFallback.facesRush(true, TrafficPriorities.Freedom, false, true, true))
    assert( ! DragoonFallback.facesRush(true, TrafficPriorities.Freedom, true, false, true))
    assert( ! DragoonFallback.facesRush(true, TrafficPriorities.Freedom, true, true, false))
  }

  test("a pure-melee pack meets a brave line, not a fallback") {
    assert( ! DragoonFallback.facesRush(
      isDragoon = true,
      pushPriority = TrafficPriorities.Freedom,
      hasRangedTarget = true,
      allRushers = true,
      enemyHasRanged = false))
  }
}
