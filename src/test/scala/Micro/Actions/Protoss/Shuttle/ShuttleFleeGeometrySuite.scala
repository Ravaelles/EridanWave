package Micro.Actions.Protoss.Shuttle

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class ShuttleFleeGeometrySuite extends AnyFunSuite {

  private val base = Pixel(0, 0)

  test("the rear is the centroid of the third nearest the base") {
    val members = Seq(Pixel(320, 0), Pixel(640, 0), Pixel(960, 0), Pixel(3200, 0))
    assert(ShuttleFleeGeometry.rearOfCluster(members, base) == Pixel(480, 0))
  }

  test("a lone member is its own rear") {
    assert(ShuttleFleeGeometry.rearOfCluster(Seq(Pixel(1000, 500)), base) == Pixel(1000, 500))
  }

  test("co-tile members tie on tile distance, input order decides") {
    val members = Seq(Pixel(320, 0), Pixel(321, 0), Pixel(960, 0))
    assert(ShuttleFleeGeometry.rearOfCluster(members, base) == Pixel(320, 0))
  }

  test("no cluster means the base itself") {
    assert(ShuttleFleeGeometry.rearOfCluster(Seq.empty, base) == base)
  }

  test("the flee threshold is stored in tiles") {
    assert(ShuttleFleeGeometry.scourgeFleeDist == 12.0)
  }

  test("the far fighter never drags the rear forward") {
    val members = Seq(Pixel(320, 0), Pixel(640, 0), Pixel(960, 0), Pixel(9600, 0))
    val rear = ShuttleFleeGeometry.rearOfCluster(members, base)
    assert(rear.x < 960 && rear.y == 0)
  }

  test("fresh Shuttles keep the flat margin") {
    assert(ShuttleFleeGeometry.safetyMarginPixels(0.0, fasterFlyer = true) == 64)
    assert(ShuttleFleeGeometry.safetyMarginPixels(0.0, fasterFlyer = false) == 160)
  }

  test("one wound curve prices the margin: nine tiles when dying") {
    assert(ShuttleFleeGeometry.woundFleeBonusTiles == 9.0)
    assert(ShuttleFleeGeometry.woundBonusTiles(0.5) == 4.5)
    assert(ShuttleFleeGeometry.safetyMarginPixels(0.5, fasterFlyer = true) == 208)
    assert(ShuttleFleeGeometry.safetyMarginPixels(1.0, fasterFlyer = false) == 448)
  }

  test("the wound curve is clamped at full wounds") {
    assert(ShuttleFleeGeometry.safetyMarginPixels(2.0, fasterFlyer = true) == 352)
    assert(ShuttleFleeGeometry.safetyMarginPixels(9.0, fasterFlyer = false) == 448)
  }


  test("melee reach never triggers the preemptive flee") {
    assert( ! ShuttleFleeGeometry.threatensPreemptively(0.0, 32.0, 9.0 * 32.0))
    assert( ! ShuttleFleeGeometry.threatensPreemptively(0.0, 16.0, 9.0 * 32.0))
  }

  test("a ranged threat inside reach plus band triggers the flee") {
    // Hydralisk reach (4 tiles) + full band (9 tiles): 12 tiles flee,
    // 14 tiles hold.
    assert(ShuttleFleeGeometry.threatensPreemptively(12.0 * 32.0, 4.0 * 32.0, 9.0 * 32.0))
    assert( ! ShuttleFleeGeometry.threatensPreemptively(14.0 * 32.0, 4.0 * 32.0, 9.0 * 32.0))
  }
}
