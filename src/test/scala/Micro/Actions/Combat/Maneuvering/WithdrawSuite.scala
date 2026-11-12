package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class WithdrawSuite extends AnyFunSuite {

  private val home = Pixel(100, 100)
  private val allyNear = Pixel(200, 200)
  private val allyFar = Pixel(300, 300)

  test("no threat reading means home") {
    assert(Withdraw.toAllies(None, Seq((allyFar, 500.0)), home) == home)
  }

  test("no safer ally means home") {
    assert(Withdraw.toAllies(Some(100.0), Seq.empty, home) == home)
    assert(Withdraw.toAllies(Some(100.0), Seq((allyNear, 120.0)), home) == home)
  }

  test("rallies to the nearest ally further than the margin") {
    val allies = Seq((allyFar, 500.0), (allyNear, 300.0))
    assert(Withdraw.toAllies(Some(100.0), allies, home) == allyNear)
  }

  test("the margin keeps barely-safer allies out") {
    assert(Withdraw.toAllies(Some(100.0), Seq((allyNear, 164.0)), home) == home)
    assert(Withdraw.toAllies(Some(100.0), Seq((allyNear, 164.1)), home) == allyNear)
  }
}
