package Micro.Actions.Protoss.Dragoon

import org.scalatest.funsuite.AnyFunSuite

class DragoonPackStandSuite extends AnyFunSuite {

  private def stands(
    dragoonCount: Int = 3,
    lightOnly: Boolean = true,
    totalHp: Int = DragoonPackStand.retreatHp + 1): Boolean =
    DragoonPackStand.shouldStand(dragoonCount, lightOnly, totalHp)

  test("a pack against light Terran holds the line") {
    assert(stands())
  }

  test("a pair is no pack") {
    assert( ! stands(dragoonCount = DragoonPackStand.packDragoons))
    assert(stands(dragoonCount = DragoonPackStand.packDragoons + 1))
  }

  test("anything heavier breaks the stand") {
    assert( ! stands(lightOnly = false))
  }

  test("a dying hull still runs") {
    assert( ! stands(totalHp = DragoonPackStand.retreatHp))
    assert(stands(totalHp = DragoonPackStand.retreatHp + 1))
  }
}
