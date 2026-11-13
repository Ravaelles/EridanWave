package ProxyBwapi.UnitInfo

import bwapi.UnitType
import org.scalatest.funsuite.AnyFunSuite
import testkit.TestKit

class GasMiningSuite extends AnyFunSuite {

  test("the fourth gas miner needs a rich workforce") {
    assert(TestKit.friendly(UnitType.Protoss_Assimilator, id = 1).richGasWorkforce == 55)
  }
}
