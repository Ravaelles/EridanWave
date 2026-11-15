package Micro.Actions.Protoss.Zealot

import org.scalatest.funsuite.AnyFunSuite

class ZealotFallbackSuite extends AnyFunSuite {

  test("a blade-line warrior falls back from speedlings in range") {
    assert(ZealotFallback.outnumberedBySpeedlings(isBladeLine = true, hasSpeedlingInRange = true))
    assert( ! ZealotFallback.outnumberedBySpeedlings(isBladeLine = false, hasSpeedlingInRange = true))
    assert( ! ZealotFallback.outnumberedBySpeedlings(isBladeLine = true, hasSpeedlingInRange = false))
  }
}
