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

  test("wounds scale the margin tenfold") {
    assert(ShuttleFleeGeometry.safetyMarginPixels(0.1, fasterFlyer = true) == 128)
    assert(ShuttleFleeGeometry.safetyMarginPixels(1.0, fasterFlyer = false) == 1760)
  }
}
