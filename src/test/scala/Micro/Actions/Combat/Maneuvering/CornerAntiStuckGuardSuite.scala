package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class CornerAntiStuckGuardSuite extends AnyFunSuite {

  test("the guard slides destinations out of the trap square") {
    assert(CornerAntiStuckGuard.guard(Pixel(100, 100), 1024, 1024) == Pixel(100, 10 * 32))
    assert(CornerAntiStuckGuard.guard(Pixel(512, 512), 1024, 1024) == Pixel(512, 512))
  }

  test("corsairs treat 20 tiles as the corner zone") {
    assert(CornerAntiStuckGuard.corsairTiles == 20.0)
  }

  test("the nearest corner is picked by quadrant") {
    assert(CornerAntiStuckGuard.nearestCorner(Pixel(100, 900), 1024, 1024) == Pixel(0, 1024))
    assert(CornerAntiStuckGuard.nearestCorner(Pixel(900, 100), 1024, 1024) == Pixel(1024, 0))
  }

  test("the zone reaches 20 tiles from the corner") {
    assert(CornerAntiStuckGuard.inCornerZone(Pixel(19 * 32, 0), 4096, 4096, 20.0))
    assert( ! CornerAntiStuckGuard.inCornerZone(Pixel(21 * 32, 0), 4096, 4096, 20.0))
  }

  test("the blended escape bends off the corner, enemies first") {
    // Threat due south, corner south-west: the pure enemy ray runs dead
    // north, so any east in the answer is the corner push working.
    val bent = CornerAntiStuckGuard.blendedEscape(Pixel(640, 100), Pixel(640, 500), Pixel(0, 0), 320.0)
    assert(bent.y < 100)
    assert(bent.x > 640)
    // Threat due east, corner due west: enemies outweigh the corner two
    // to one, so the escape still runs from the threat, not the corner.
    val east = CornerAntiStuckGuard.blendedEscape(Pixel(500, 500), Pixel(900, 500), Pixel(100, 500), 300.0)
    assert(east == Pixel(400, 500))
  }
}
