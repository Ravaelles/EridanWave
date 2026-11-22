package Debugging

import ProxyBwapi.Orders
import org.scalatest.funsuite.AnyFunSuite

/**
 * Wasted-volley ledger (kite discipline, owner direction: never yield
 * ground for free): frames where a warrior's gun is loaded and a
 * hittable enemy stands inside its reach, yet the frame ends moving.
 * The plant holds the last cooldown frames for the volley instead of
 * donating them; the ledger must fall while kills do not. Counted per
 * unit, reported once - the Motion audit and the Act chain bracket
 * the frames, the summary names the units.
 */
class VolleyWasteSuite extends AnyFunSuite {

  test("clean ledger reports the all-clear") {
    VolleyWaste.reset()
    assert(VolleyWaste.summary().contains("none"))
    assert(VolleyWaste.worstTotal == 0)
  }

  test("a loaded gun walking past a hittable enemy is a forgone volley") {
    assert(VolleyWaste.isForgone(cooldownLeft = 0, hasHittable = true, order = Orders.Move, isWarrior = true))
    assert(VolleyWaste.isForgone(cooldownLeft = 0, hasHittable = true, order = Orders.MoveToGas, isWarrior = true))
  }

  test("cooling guns, empty reach, shooting and holding hulls are never waste") {
    assert( ! VolleyWaste.isForgone(cooldownLeft = 5, hasHittable = true, order = Orders.Move, isWarrior = true))
    assert( ! VolleyWaste.isForgone(cooldownLeft = 0, hasHittable = false, order = Orders.Move, isWarrior = true))
    assert( ! VolleyWaste.isForgone(cooldownLeft = 0, hasHittable = true, order = Orders.AttackUnit, isWarrior = true))
    assert( ! VolleyWaste.isForgone(cooldownLeft = 0, hasHittable = true, order = Orders.AttackMove, isWarrior = true))
    assert( ! VolleyWaste.isForgone(cooldownLeft = 0, hasHittable = true, order = Orders.HoldPosition, isWarrior = true))
    assert( ! VolleyWaste.isForgone(cooldownLeft = 0, hasHittable = true, order = Orders.Stop, isWarrior = true))
  }

  test("unwarmed hulls never count") {
    assert( ! VolleyWaste.isForgone(cooldownLeft = 0, hasHittable = true, order = Orders.Move, isWarrior = false))
  }

  test("bumps accumulate per unit and name the worst") {
    VolleyWaste.reset()
    VolleyWaste.bump(7, "Dragoon", cooldownLeft = 0, hasHittable = true, order = Orders.Move, isWarrior = true, frame = 0)
    VolleyWaste.bump(7, "Dragoon", cooldownLeft = 0, hasHittable = true, order = Orders.Move, isWarrior = true, frame = 0)
    VolleyWaste.bump(7, "Dragoon", cooldownLeft = 0, hasHittable = true, order = Orders.Move, isWarrior = true, frame = 100)
    VolleyWaste.bump(7, "Dragoon", cooldownLeft = 0, hasHittable = true, order = Orders.Move, isWarrior = true, frame = 101)
    VolleyWaste.bump(7, "Dragoon", cooldownLeft = 8, hasHittable = true, order = Orders.Move, isWarrior = true, frame = 102)
    VolleyWaste.bump(9, "Zealot", cooldownLeft = 0, hasHittable = true, order = Orders.Move, isWarrior = true, frame = 0)
    val report = VolleyWaste.summary()
    assert(report.contains("Dragoon #7"))
    assert(report.contains("1f"))
    assert(VolleyWaste.worstTotal == 1)
  }

  test("lone flickers never count, only repeated stale moves") {
    VolleyWaste.reset()
    VolleyWaste.bump(7, "Dragoon", cooldownLeft = 0, hasHittable = true, order = Orders.Move, isWarrior = true, frame = 100)
    assert(VolleyWaste.worstTotal == 0)
    VolleyWaste.bump(7, "Dragoon", cooldownLeft = 0, hasHittable = true, order = Orders.Move, isWarrior = true, frame = 103)
    assert(VolleyWaste.worstTotal == 0)
    VolleyWaste.bump(7, "Dragoon", cooldownLeft = 0, hasHittable = true, order = Orders.Move, isWarrior = true, frame = 104)
    assert(VolleyWaste.worstTotal == 1)
    VolleyWaste.bump(7, "Dragoon", cooldownLeft = 0, hasHittable = true, order = Orders.HoldPosition, isWarrior = true, frame = 105)
    VolleyWaste.bump(7, "Dragoon", cooldownLeft = 0, hasHittable = true, order = Orders.Move, isWarrior = true, frame = 106)
    assert(VolleyWaste.worstTotal == 1)
  }

  test("fresh orders are forgiven until the application window lapses") {
    assert( ! VolleyWaste.isStaleOrder(sinceFrame = 0, nowFrame = 0, staleFrames = 3))
    assert( ! VolleyWaste.isStaleOrder(sinceFrame = 0, nowFrame = 2, staleFrames = 3))
    assert(VolleyWaste.isStaleOrder(sinceFrame = 0, nowFrame = 3, staleFrames = 3))
    assert(VolleyWaste.isStaleOrder(sinceFrame = 10, nowFrame = 100, staleFrames = 3))
  }
}
