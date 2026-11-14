package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class ReaverBraveSuite extends AnyFunSuite {

  private def applies(
    health        : Int  = 150,
    escorted      : Boolean = true,
    targetInReach : Boolean = true,
    cooldownLeft  : Int  = 0,
    isReaver      : Boolean = true): Boolean =
    ReaverBrave.applies(isReaver, health, escorted, targetInReach, cooldownLeft)

  test("a healthy escorted Reaver with a target in reach and a loaded scarab stands") {
    assert(applies())
    assert(applies(health = 120))
  }

  test("the Reaver rule never fires for other units") {
    assert( ! applies(isReaver = false))
  }

  test("a wounded or dry Reaver still backs off") {
    assert( ! applies(health = 119))
    assert( ! applies(cooldownLeft = 1))
  }

  test("no escort means no stand") {
    assert( ! applies(escorted = false))
  }

  test("nothing in scarab reach means no stand") {
    assert( ! applies(targetInReach = false))
  }
}
