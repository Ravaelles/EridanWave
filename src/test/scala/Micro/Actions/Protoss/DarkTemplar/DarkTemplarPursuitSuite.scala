package Micro.Actions.Protoss.DarkTemplar

import org.scalatest.funsuite.AnyFunSuite

class DarkTemplarPursuitSuite extends AnyFunSuite {

  test("an arrived-but-unseen ghost is dropped, nothing else is") {
    assert(DarkTemplarPursuit.shouldDropGhost(distTilesToGhost = 0.0, ghostVisible = false))
    assert(DarkTemplarPursuit.shouldDropGhost(distTilesToGhost = 4.0, ghostVisible = false))
    assert( ! DarkTemplarPursuit.shouldDropGhost(distTilesToGhost = 4.01, ghostVisible = false))
    assert( ! DarkTemplarPursuit.shouldDropGhost(distTilesToGhost = 40.0, ghostVisible = false))
    assert( ! DarkTemplarPursuit.shouldDropGhost(distTilesToGhost = 0.0, ghostVisible = true))
  }

  test("a contactless hunter past the band rejoins a fighting squad") {
    assert(DarkTemplarPursuit.shouldRejoinSquad(distTilesToSquad = 14.01, squadFighting = true, hasLocalContact = false))
    assert( ! DarkTemplarPursuit.shouldRejoinSquad(distTilesToSquad = 14.0, squadFighting = true, hasLocalContact = false))
    assert( ! DarkTemplarPursuit.shouldRejoinSquad(distTilesToSquad = 40.0, squadFighting = false, hasLocalContact = false))
    assert( ! DarkTemplarPursuit.shouldRejoinSquad(distTilesToSquad = 40.0, squadFighting = true, hasLocalContact = true))
  }

  test("a dropped ghost stays dropped until re-seen") {
    val unitId = 901
    val ghostId = 902
    // Fresh contact, far away: walking, not dropped.
    assert( ! DarkTemplarPursuit.ghostDropped(unitId, ghostId, arrivedUnseen = false, recentlySeen = true))
    // Walked blind past the fresh window: still walking, not dropped.
    assert( ! DarkTemplarPursuit.ghostDropped(unitId, ghostId, arrivedUnseen = false, recentlySeen = false))
    // Arrival-to-nothing: dropped.
    assert(DarkTemplarPursuit.ghostDropped(unitId, ghostId, arrivedUnseen = true, recentlySeen = false))
    // Same fix one frame later, backed off past the radius: stays dropped.
    assert(DarkTemplarPursuit.ghostDropped(unitId, ghostId, arrivedUnseen = false, recentlySeen = false))
    // Re-sighted: released, huntable again.
    assert( ! DarkTemplarPursuit.ghostDropped(unitId, ghostId, arrivedUnseen = false, recentlySeen = true))
  }
}
