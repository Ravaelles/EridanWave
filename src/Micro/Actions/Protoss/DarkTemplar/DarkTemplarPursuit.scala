package Micro.Actions.Protoss.DarkTemplar

import Tactic.Squads.Decisions.ClusterCohesion
import Utilities.Latch

/**
 * Dark Templar pursuit validity (no game access): a remembered contact
 * is a fix that ages - walking to it is the hunt, standing on its empty
 * pixel is feeding a ghost. A ghost the Templar has already closed on
 * without re-seeing is dropped, so the hunt holds and the squad phase
 * owns the hull again instead of the memory. The drop is arrival-gated,
 * never time-gated: long walks to far fixes keep walking, only
 * arrival-to-nothing lets go. The rejoin below answers the same where
 * for UMS and lone squads the cluster cohesion leg never reaches
 * (it is macro-game only by design): a contactless hunter past the
 * drift band returns to a fighting squad instead of holding the open.
 */
object DarkTemplarPursuit {

  /**
   * Arrival radius around the ghost pixel, in tiles: closed this far
   * with still no eyes, the fix is stale.
   */
  val arriveTiles: Double = 4.0

  /**
   * Fresh-contact window, in frames: seen this recently, the fix is a
   * walk, not a ghost - it releases a stuck drop (a re-sighted enemy
   * re-arms the hunt with its new lastSeen).
   */
  val recentSeenFrames: Int = 24

  private val droppedGhosts = new Latch

  private def ghostKey(unitId: Int, ghostId: Int): Int = unitId * 100003 + ghostId

  /**
   * Sticky ghost drop (latched per hunter-ghost pair, cleared with every
   * game like all latches): fires on arrival-to-nothing, releases on
   * fresh contact. A dropped ghost stays unhuntable until re-seen, so
   * the hunt cannot re-grab the same fix it just walked away from -
   * without the stick the drop and the walk ping-pong on the arrival
   * radius every frame.
   */
  def ghostDropped(unitId: Int, ghostId: Int, arrivedUnseen: Boolean, recentlySeen: Boolean): Boolean =
    droppedGhosts(ghostKey(unitId, ghostId), fires = arrivedUnseen, releases = recentlySeen)

  /**
   * Drop the ghost (pure flags for the suite): unseen and already
   * closed on. A seen fix is a fight, not a memory; a far fix is a
   * walk, not an arrival.
   */
  def shouldDropGhost(distTilesToGhost: Double, ghostVisible: Boolean): Boolean =
    ! ghostVisible && distTilesToGhost <= arriveTiles

  /**
   * Rejoin the fighting squad (pure flags for the suite): no local
   * contact, the squad fights without us, past the shared drift band
   * (the same fourteen tiles `ClusterCohesion` owns - one band, two
   * callers, the UMS-exempt leg and this hunt arm).
   */
  def shouldRejoinSquad(
    distTilesToSquad: Double,
    squadFighting: Boolean,
    hasLocalContact: Boolean): Boolean =
    squadFighting && ! hasLocalContact && distTilesToSquad > ClusterCohesion.maxDriftTiles
}
