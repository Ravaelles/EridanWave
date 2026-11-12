package Information.Scouting

import org.scalatest.funsuite.AnyFunSuite

class SneakyWatchSuite extends AnyFunSuite {

  test("ordinary units stay quiet") {
    assert(SneakyWatch.sneakyLabel(isLurker = false, isDarkTemplar = false, isWraith = false, isGhost = false).isEmpty)
  }

  test("each sneaky class has its trace label") {
    assert(SneakyWatch.sneakyLabel(isLurker = true, isDarkTemplar = false, isWraith = false, isGhost = false).contains("Lurkers"))
    assert(SneakyWatch.sneakyLabel(isLurker = false, isDarkTemplar = true, isWraith = false, isGhost = false).contains("DarkTemplars"))
    assert(SneakyWatch.sneakyLabel(isLurker = false, isDarkTemplar = false, isWraith = true, isGhost = false).contains("Wraiths"))
    assert(SneakyWatch.sneakyLabel(isLurker = false, isDarkTemplar = false, isWraith = true, isGhost = true).contains("Wraiths"))
  }
}
