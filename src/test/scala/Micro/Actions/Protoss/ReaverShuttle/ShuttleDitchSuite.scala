package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

/**
 * Hitchhiker-ditch discipline: a shuttle never dumps cargo under guns.
 * Aboard a flying hull beats standing under them - the dump waits for
 * clear ground. Pure tiles for the suite; the leg reads the drop point.
 */
class ShuttleDitchSuite extends AnyFunSuite {

  test("clear ground takes the cargo, guns keep it aboard") {
    assert(ShuttleDitch.clearToLand(nearestGroundThreatTiles = Double.PositiveInfinity))
    assert(ShuttleDitch.clearToLand(nearestGroundThreatTiles = 10.1))
    assert( ! ShuttleDitch.clearToLand(nearestGroundThreatTiles = 10.0))
    assert( ! ShuttleDitch.clearToLand(nearestGroundThreatTiles = 0.0))
  }
}
