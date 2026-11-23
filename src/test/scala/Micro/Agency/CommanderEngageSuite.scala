package Micro.Agency

import org.scalatest.funsuite.AnyFunSuite

class CommanderEngageSuite extends AnyFunSuite {

  test("inside the designation margin the ladder names the victim") {
    assert(CommanderEngage.shouldDesignate(pixelsToGetInRange = 0.0, rangePixels = 128.0))
    assert(CommanderEngage.shouldDesignate(pixelsToGetInRange = 19.0, rangePixels = 128.0))
    assert( ! CommanderEngage.shouldDesignate(pixelsToGetInRange = 20.0, rangePixels = 128.0))
  }

  test("beyond the margin the walk keeps its attack-move") {
    assert( ! CommanderEngage.shouldDesignate(pixelsToGetInRange = 200.0, rangePixels = 128.0))
  }
}
