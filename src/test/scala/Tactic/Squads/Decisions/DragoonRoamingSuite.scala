package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class DragoonRoamingSuite extends AnyFunSuite {

  test("dragoons roam the unseen mirror") {
    assert(DragoonRoaming.mayRoam(isDragoon = true, enemyIsProtoss = true, enemyDragoonSeen = false))
  }

  test("the permission closes on first recognition, and never opened elsewhere") {
    assert( ! DragoonRoaming.mayRoam(isDragoon = true, enemyIsProtoss = true, enemyDragoonSeen = true))
    assert( ! DragoonRoaming.mayRoam(isDragoon = true, enemyIsProtoss = false, enemyDragoonSeen = false))
    assert( ! DragoonRoaming.mayRoam(isDragoon = false, enemyIsProtoss = true, enemyDragoonSeen = false))
  }
}
