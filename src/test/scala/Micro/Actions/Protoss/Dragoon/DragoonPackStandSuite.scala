package Micro.Actions.Protoss.Dragoon

import org.scalatest.funsuite.AnyFunSuite

class DragoonPackStandSuite extends AnyFunSuite {

  private def stands(
    enemyIsTerran: Boolean = true,
    dragoonCount: Int = 3,
    lightOnly: Boolean = true,
    totalHp: Int = DragoonPackStand.retreatHp + 1): Boolean =
    DragoonPackStand.shouldStand(enemyIsTerran, dragoonCount, lightOnly, totalHp)

  test("a pack against light Terran holds the line") {
    assert(stands())
  }

  test("another race is another doctrine: the Terran gate stays shut") {
    assert( ! stands(enemyIsTerran = false))
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
