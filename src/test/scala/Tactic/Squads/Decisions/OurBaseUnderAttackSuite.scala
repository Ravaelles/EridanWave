package Tactic.Squads.Decisions

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class OurBaseUnderAttackSuite extends AnyFunSuite {

  private val home = Pixel(3200, 3200)

  test("eight tiles mark the inside") {
    assert(OurBaseUnderAttack.enemiesInside(home, Seq(Pixel(3200 + 8 * 32, 3200))) == 1)
    assert(OurBaseUnderAttack.enemiesInside(home, Seq(Pixel(3200 + 9 * 32, 3200))) == 0)
  }

  test("a single enemy combat unit breaches") {
    assert(OurBaseUnderAttack.atLeastSingleEnemy(home, Seq(Pixel(3200 + 32, 3200))))
    assert( ! OurBaseUnderAttack.atLeastSingleEnemy(home, Seq(Pixel(3200 + 30 * 32, 3200))))
    assert( ! OurBaseUnderAttack.atLeastSingleEnemy(home, Seq.empty))
  }

  test("carriers answer two or more ground enemies") {
    assert(OurBaseUnderAttack.atLeastTwoGroundEnemies(home, Seq(Pixel(3200 + 32, 3200), Pixel(3200, 3200 + 64))))
    assert( ! OurBaseUnderAttack.atLeastTwoGroundEnemies(home, Seq(Pixel(3200 + 32, 3200))))
    assert( ! OurBaseUnderAttack.atLeastTwoGroundEnemies(home, Seq.empty))
  }
}
