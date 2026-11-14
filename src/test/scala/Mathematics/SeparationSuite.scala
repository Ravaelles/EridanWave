package Mathematics

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class SeparationSuite extends AnyFunSuite {

  private def neighbor(id: Int, x: Int, y: Int, distTiles: Double): Separation.Neighbor =
    Separation.Neighbor(id, Pixel(x, y), distTiles)

  test("only the newer hull moves, and only inside the radius") {
    assert(Separation.shouldSpread(usId = 5, olderId = 4, distTiles = 4.9, minTiles = 5.0))
    assert( ! Separation.shouldSpread(usId = 5, olderId = 4, distTiles = 5.0, minTiles = 5.0))
    assert( ! Separation.shouldSpread(usId = 4, olderId = 5, distTiles = 1.0, minTiles = 5.0))
  }

  test("the nearest older neighbor is the partner") {
    val others = Seq(neighbor(9, 0, 0, 1.0), neighbor(3, 0, 0, 8.0), neighbor(7, 0, 0, 2.0))
    assert(Separation.partner(usId = 7, others).exists(_.id == 3))
    assert(Separation.partner(usId = 1, others).isEmpty)
  }

  test("separation lands at the radius, on the ray from the older hull") {
    val away = Separation.awayFrom(Pixel(64, 0), Pixel(0, 0), minTiles = 9.0)
    assert(away.x == 9 * 32)
    assert(away.y == 0)
  }

  test("a shooting or committed hull holds its firing line") {
    assert(Separation.holdForAttack(hasTarget = true, coolingDown = false))
    assert(Separation.holdForAttack(hasTarget = false, coolingDown = true))
    assert(Separation.holdForAttack(hasTarget = true, coolingDown = true))
    assert( ! Separation.holdForAttack(hasTarget = false, coolingDown = false))
  }
}
