package Micro.Formation

import Mathematics.Points.Pixel
import Micro.Formation.WallGapGeometry.{Body, Box, Gap}
import org.scalatest.funsuite.AnyFunSuite

class WallGapGeometrySuite extends AnyFunSuite {

  test("boxes measure overlap and containment") {
    val box = Box(0, 0, 32, 64)
    assert(box.center == Pixel(16, 32))
    assert(box.expand(8) == Box(-8, -8, 40, 72))
    assert(box.intersects(Box(31, 0, 64, 64)))
    assert( ! box.intersects(Box(32, 0, 64, 64)))
    assert(box.touches(Box(32, 0, 64, 64)))
    assert(box.contains(Pixel(0, 0)))
    assert( ! box.contains(Pixel(32, 32)))
  }

  test("bodies forbid their collision footprint") {
    val body = Body(5, 5, 5, 5)
    assert(body.at(Pixel(100, 100)) == Box(95, 95, 105, 105))
    assert(body.forbidden(Box(0, 0, 32, 32)) == Box(-4, -4, 37, 37))
    assert(body.extent(-1.0, 0.0) == 5.0)
    assert(body.extent(1.0, 0.0) == 5.0)
  }

  test("connected boxes link through touching, not overlap") {
    val a = Box(0, 0, 32, 32)
    val b = Box(32, 0, 64, 32)
    val c = Box(64, 0, 96, 32)
    val d = Box(200, 200, 232, 232)
    assert(WallGapGeometry.connectedTo(a, Vector(b, c, d)) == Vector(a, b, c))
  }

  private val ling = Body(5, 5, 5, 5)

  private def corridor(): Option[Gap] =
    WallGapGeometry.between(
      Box(0, 0, 32, 64),
      Box(96, 0, 128, 64),
      Pixel(64, 500),
      None,
      Vector.empty,
      _ => false,
      ling,
      (_, _, _) => true)

  test("a corridor between two boxes faces the base") {
    val gap = corridor()
    assert(gap.isDefined)
    assert(gap.get.nx == 0.0)
    assert(gap.get.ny == 1.0)
    assert(gap.get.width == 64)
  }

  test("an impassable corridor is no gap") {
    val gap = WallGapGeometry.between(
      Box(0, 0, 32, 64),
      Box(96, 0, 128, 64),
      Pixel(64, 500),
      None,
      Vector.empty,
      _ => false,
      ling,
      (_, _, _) => false)
    assert(gap.isEmpty)
  }

  test("shared passages dedup to one entrance, keeping the doorway") {
    val a = Box(0, 0, 32, 64)
    val plain = corridor().get
    val door = plain.copy(doorway = true)
    val merged = WallGapGeometry.dedupGaps(Vector(plain, door))
    assert(merged.size == 1)
    assert(merged.head.doorway)
    val apart = Gap(Box(400, 400, 432, 464), Box(496, 400, 528, 464), Pixel(432, 432), Pixel(496, 432), 0.0, 1.0, -32.0, 32.0, false)
    assert(WallGapGeometry.dedupGaps(Vector(plain, apart)).size == 2)
  }
}
