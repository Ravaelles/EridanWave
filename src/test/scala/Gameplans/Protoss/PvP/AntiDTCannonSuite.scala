package Gameplans.Protoss.PvP

import Mathematics.Points.{Pixel, Tile}
import org.scalatest.funsuite.AnyFunSuite

class AntiDTCannonSuite extends AnyFunSuite {

  test("the choke cannon sits five tiles off, inside a five-to-six ring") {
    assert(AntiDTCannon.chokeCannonTiles == 5.0)
    assert(AntiDTCannon.chokeRingNearTiles == 5)
    assert(AntiDTCannon.chokeRingFarTiles == 6)
  }

  test("ring membership takes the edges, rejects both sides") {
    assert( ! AntiDTCannon.inRing(4))
    assert(AntiDTCannon.inRing(5))
    assert(AntiDTCannon.inRing(6))
    assert( ! AntiDTCannon.inRing(7))
  }

  test("a straight-line anchor five tiles out lands in the ring") {
    val choke = Pixel(3200, 3200)
    val main  = Pixel(3200, 3200 + 32 * 40)
    val tile  = choke.project(main, AntiDTCannon.chokeCannonTiles * 32.0).tile
    assert(tile.tileDistanceChebyshev(Tile(100, 100)) == 5)
  }

  test("urgent Forge fires on suspicion with no Forge, never twice") {
    assert(AntiDTCannon.urgentForgeNow(dtLikely = true, haveForge = false))
    assert( ! AntiDTCannon.urgentForgeNow(dtLikely = true, haveForge = true))
    assert( ! AntiDTCannon.urgentForgeNow(dtLikely = false, haveForge = false))
  }

  test("our Citadel waits for the Forge, and for our own DT plan") {
    assert(AntiDTCannon.urgentCitadelNow(dtLikely = true, onDTPlan = true, haveForge = true, haveCitadel = false))
    assert( ! AntiDTCannon.urgentCitadelNow(dtLikely = true, onDTPlan = true, haveForge = false, haveCitadel = false))
    assert( ! AntiDTCannon.urgentCitadelNow(dtLikely = true, onDTPlan = false, haveForge = true, haveCitadel = false))
    assert( ! AntiDTCannon.urgentCitadelNow(dtLikely = true, onDTPlan = true, haveForge = true, haveCitadel = true))
    assert( ! AntiDTCannon.urgentCitadelNow(dtLikely = false, onDTPlan = true, haveForge = true, haveCitadel = false))
  }

  test("our Archives waits for the Citadel behind the Forge") {
    assert(AntiDTCannon.urgentArchivesNow(dtLikely = true, onDTPlan = true, haveForge = true, haveCitadel = true, haveArchives = false))
    assert( ! AntiDTCannon.urgentArchivesNow(dtLikely = true, onDTPlan = true, haveForge = true, haveCitadel = false, haveArchives = false))
    assert( ! AntiDTCannon.urgentArchivesNow(dtLikely = true, onDTPlan = true, haveForge = false, haveCitadel = true, haveArchives = false))
    assert( ! AntiDTCannon.urgentArchivesNow(dtLikely = false, onDTPlan = true, haveForge = true, haveCitadel = true, haveArchives = false))
  }
}
