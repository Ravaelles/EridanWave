package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class AvoidDangerousUnitsSuite extends AnyFunSuite {

  test("a charged Ghost inside lockdown reach turns the hull") {
    assert(AvoidDangerousUnits.isLockdownThreat(ghostEnergy = 98, distTiles = 9.5))
    assert(AvoidDangerousUnits.isLockdownThreat(ghostEnergy = 200, distTiles = 0.0))
  }

  test("a Ghost with our id on its order target turns the hull at any range") {
    assert(AvoidDangerousUnits.isTargetedBy(Some(7), 7))
    assert( ! AvoidDangerousUnits.isTargetedBy(Some(8), 7))
    assert( ! AvoidDangerousUnits.isTargetedBy(None, 7))
  }

  test("a dry Ghost or a safe distance never turns the hull") {
    assert( ! AvoidDangerousUnits.isLockdownThreat(ghostEnergy = 97, distTiles = 0.0))
    assert( ! AvoidDangerousUnits.isLockdownThreat(ghostEnergy = 200, distTiles = 11.1))
    assert( ! AvoidDangerousUnits.isLockdownThreat(ghostEnergy = 0, distTiles = 50.0))
  }

  test("the fallback lands past the line, straight off the Ghost") {
    assert(AvoidDangerousUnits.fallback(Pixel(0, 0), Pixel(96, 0)) == Pixel(-387, 0))
  }
}
