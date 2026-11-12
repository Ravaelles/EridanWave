package Gameplans.Protoss.PvZ.reactive

import org.scalatest.funsuite.AnyFunSuite

class PvZArmyWeightsSuite extends AnyFunSuite {

  private def seen(
    zerglings: Int = 0,
    hydralisks: Int = 0,
    mutalisks: Int = 0,
    sunkenColonies: Int = 0,
    confidenceDefending01: Double = 1.0,
    highTemplars: Int = 0,
    gas: Int = 0,
    hydralisksLikely: Boolean = false,
    mutalisksLikely: Boolean = false
  ) = PvZArmyWeights.Seen(zerglings, hydralisks, mutalisks, sunkenColonies,
    confidenceDefending01, highTemplars, gas, hydralisksLikely, mutalisksLikely)

  private def close(actual: Double, expected: Double): Unit =
    assert(Math.abs(actual - expected) < 1e-9, s"$actual != $expected")

  test("an empty field still assumes eight Zerglings and weights Zealots alone") {
    val mix = PvZArmyWeights.of(seen())
    assert(mix.zerglings == 8)
    close(mix.zealots, 1.0)
    close(mix.dragoons, 0.0)
  }

  test("expected Hydralisks are counted at four and suppress the Zergling assumption") {
    val mix = PvZArmyWeights.of(seen(hydralisksLikely = true))
    assert(mix.hydralisks == 4)
    assert(mix.zerglings == 0)
    close(mix.dragoons, 1.0)
  }

  test("expected Mutalisks are counted at eight unless Hydralisks are expected") {
    assert(PvZArmyWeights.of(seen(mutalisksLikely = true)).mutalisks == 8)
    assert(PvZArmyWeights.of(seen(mutalisksLikely = true, hydralisksLikely = true)).mutalisks == 0)
  }

  test("sunken colonies only count once we are defending") {
    close(PvZArmyWeights.of(seen(sunkenColonies = 2, confidenceDefending01 = 0.5)).sunkenColonies, 0.0)
    close(PvZArmyWeights.of(seen(sunkenColonies = 2, confidenceDefending01 = 1.0)).sunkenColonies, 2.0)
  }

  test("weights are normalized across the four Zerg unit types") {
    // Four seen Hydralisks with no Zerglings seen: Zerglings assumed at 8,
    // so 0.25 * 8 = 2 against 1.0 * 4 = 4.
    val mix = PvZArmyWeights.of(seen(hydralisks = 4))
    close(mix.zealots, 2.0 / 6)
    close(mix.dragoons, 4.0 / 6)
    close(mix.zealots + mix.dragoons, 1.0)
  }

  test("reavers weigh Hydralisks plus Sunkens and Corsairs weigh Mutalisks") {
    val mix = PvZArmyWeights.of(seen(hydralisks = 4, sunkenColonies = 2, mutalisks = 4))
    // 0.25*8 + 1.0*4 + 2.0*4 + 4.0*2 = 2 + 4 + 8 + 8 = 22
    close(mix.reavers, 12.0 / 22)
    close(mix.corsairs, 8.0 / 22)
    close(mix.archons, 10.0 / 22)
  }

  test("High Templars and banked gas add Archon weight") {
    // One Templar plus two gas-bank units (300 gas past the first hundred).
    close(PvZArmyWeights.of(seen(highTemplars = 1, gas = 400)).archons, 4.0)
    // Two Templars pair up: 2 % 2 = 0, so only the two bank units count on top.
    close(PvZArmyWeights.of(seen(highTemplars = 2, gas = 400)).archons, 3.0)
    // 250 gas funds one more Archon past the first hundred.
    close(PvZArmyWeights.of(seen(gas = 250)).archons, 2.0)
    // Under a hundred gas nothing is banked.
    close(PvZArmyWeights.of(seen(gas = 90)).archons, 1.0)
  }
}
