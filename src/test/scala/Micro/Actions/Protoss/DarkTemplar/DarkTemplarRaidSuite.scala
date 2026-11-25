package Micro.Actions.Protoss.DarkTemplar

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarRaidSuite extends AnyFunSuite {

  test("workers inside a tile and a twentieth always die first") {
    assert(DarkTemplarRaid.workerDefaultTiles == 1.05)
    assert(DarkTemplarRaid.shouldTakeWorker(0.0))
    assert(DarkTemplarRaid.shouldTakeWorker(1.05))
    assert( ! DarkTemplarRaid.shouldTakeWorker(1.06))
  }

  test("guns are avoided inside their close band, never dived") {
    assert(DarkTemplarRaid.shouldAvoidCannon(distTiles = 8.0, dangerTiles = 7.0))
    assert(DarkTemplarRaid.shouldAvoidCannon(distTiles = 4.0, dangerTiles = 7.0))
    assert( ! DarkTemplarRaid.shouldAvoidCannon(distTiles = 8.2, dangerTiles = 7.0))
  }

  test("the escape lands past the release band, on the away ray") {
    val out = DarkTemplarRaid.escapeFrom(Pixel(0, 0), Pixel(96, 0), 7.0)
    assert(out.x < 0 && out.y == 0)
    val distTiles = Pixel(0, 0).pixelDistance(out) / 32.0
    assert(distTiles > 7.0 + 1.1 + 1.0)
    assert(Math.abs(DarkTemplarRaid.releaseDistTiles(7.0) - 9.1) < 1e-9)
  }
}
