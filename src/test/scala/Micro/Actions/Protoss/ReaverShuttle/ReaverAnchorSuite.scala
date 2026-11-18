package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class ReaverAnchorSuite extends AnyFunSuite {

  test("a fresh hull holds the pack line") {
    assert( ! ReaverAnchor.shouldBackToPack(reaverMainTiles = 100.0, armyMainTiles = 100.0, wound01 = 0.0))
    assert(ReaverAnchor.shouldBackToPack(reaverMainTiles = 100.1, armyMainTiles = 100.0, wound01 = 0.0))
  }

  test("a wounded hull keeps extra distance home") {
    assert( ! ReaverAnchor.shouldBackToPack(reaverMainTiles = 96.0, armyMainTiles = 100.0, wound01 = 1.0))
    assert(ReaverAnchor.shouldBackToPack(reaverMainTiles = 96.1, armyMainTiles = 100.0, wound01 = 1.0))
    assert( ! ReaverAnchor.shouldBackToPack(reaverMainTiles = 98.0, armyMainTiles = 100.0, wound01 = 0.5))
    assert(ReaverAnchor.shouldBackToPack(reaverMainTiles = 98.1, armyMainTiles = 100.0, wound01 = 0.5))
  }

  test("a hull behind the pack fights on") {
    assert( ! ReaverAnchor.shouldBackToPack(reaverMainTiles = 90.0, armyMainTiles = 100.0, wound01 = 1.0))
  }
}
