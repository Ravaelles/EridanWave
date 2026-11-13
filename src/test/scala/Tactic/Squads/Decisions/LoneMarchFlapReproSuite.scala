package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

/**
 * TEMPORARY go-stop reproduction - do not land, do not fix here.
 *
 * Combat units stutter march-stop-march tied to LoneMarch. Mechanism:
 * marchTerminus switches between two distant attractors - the squad goal
 * (LoneMarch, outside ten tiles) and the pack legs (rally/join/dress,
 * inside) - on a strict threshold with no deadband. Marching units cross
 * the boundary continuously, so the terminus jerks between far and near
 * every few frames: go, stop, go, stop.
 */
class LoneMarchFlapReproSuite extends AnyFunSuite {

  test("one step of movement flips the march branch") {
    // A unit closing in: 10.01 tiles out it marches alone at the goal...
    assert(LoneMarch.shouldMarchAlone(10.01))
    // ...one step later, 9.99 out, it falls into the pack legs.
    assert( ! LoneMarch.shouldMarchAlone(9.99))
    // Same discontinuity leaving: no deadband either way.
    assert(LoneMarch.shouldMarchAlone(10.01))
    assert( ! LoneMarch.shouldMarchAlone(9.99))
  }

  test("two competing attractors chatter across the boundary") {
    // 1-D march toward a far goal: outside ten tiles of the pack the unit
    // heads for the goal, inside it holds with the pack legs while the
    // pack itself advances. Relative distance breathes across ten tiles,
    // so the branch flips again and again instead of settling.
    var unit = 0.0
    var pack = 11.0
    var flips = 0
    var wasLone = LoneMarch.shouldMarchAlone(pack - unit)
    var frame = 0
    while (frame < 60) {
      val lone = LoneMarch.shouldMarchAlone(pack - unit)
      if (lone != wasLone) flips += 1
      wasLone = lone
      if (lone) unit += 1.0 else unit += 0.2
      pack += 0.6
      frame += 1
    }
    // Documents the chatter: flips repeatedly instead of settling.
    assert(flips >= 3, f"expected march chatter, saw $flips flips")
  }
}
