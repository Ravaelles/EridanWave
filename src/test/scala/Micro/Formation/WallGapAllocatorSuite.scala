package Micro.Formation

import Mathematics.Points.Pixel
import Micro.Formation.WallGapGeometry.{Body, Box, Defender, Gap}
import org.scalatest.funsuite.AnyFunSuite

class WallGapAllocatorSuite extends AnyFunSuite {

  private val ling = Body(5, 5, 5, 5)
  private val zealot = Body(15, 15, 16, 16)
  private val always = (_: Body, _: Pixel, _: Int) => true
  private val anywhere = (_: Body, _: Pixel) => true

  private def corridor(): Gap =
    WallGapGeometry.between(
      Box(0, 0, 32, 64),
      Box(96, 0, 128, 64),
      Pixel(64, 500),
      None,
      Vector.empty,
      _ => false,
      ling,
      (_, _, _) => true).get

  test("a corridor seals with defenders and reports its minimum") {
    val gap = corridor()
    val slots = new WallGapSlots(ling, always, anywhere)
    val allocator = new WallGapAllocator(Vector(gap), ling, zealot, slots, anywhere, always)

    assert(allocator.minimumDefenders.exists(_ > 0))
  }

  test("assigned defenders stand on slots of their gap") {
    val gap = corridor()
    val slots = new WallGapSlots(ling, always, anywhere)
    val allocator = new WallGapAllocator(Vector(gap), ling, zealot, slots, anywhere, always)
    val defenders = Vector(
      Defender(1, zealot, probe = false, Pixel(64, 200)),
      Defender(2, zealot, probe = false, Pixel(64, 300)))
    val assigned = allocator.assign(defenders)

    assert(assigned.keySet == Set(1, 2))
    assigned.values.foreach(p => assert(p.x >= 0 && p.x <= 128))
  }

  test("no gaps need no defenders") {
    val slots = new WallGapSlots(ling, always, anywhere)
    val allocator = new WallGapAllocator(Vector.empty, ling, zealot, slots, anywhere, always)

    assert(allocator.minimumDefenders.contains(0))
    assert(allocator.assign(Vector.empty).isEmpty)
  }
}
