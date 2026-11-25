package Gameplans.Protoss.PvP

import Mathematics.Points.Pixel

/**
 * Anti-DT cannon doctrine (pure numbers and flags, no game access):
 * where the choke cannon sits and when the emergency Forge jumps the
 * queue. Pinned by AntiDTCannonSuite; the game-coupled callers
 * (PvPIdeas.requestChokeCannon, urgentForgeVsDT) only translate live
 * state into these predicates, so no two paths can disagree about the
 * ring or the order again.
 */
object AntiDTCannon {

  /** Choke cannon anchor, in tiles off the main choke toward the main. */
  val chokeCannonTiles: Double = 5.0

  /** Ring inner edge, in tiles (Chebyshev) from the main choke center. */
  val chokeRingNearTiles: Int = 5

  /** Ring outer edge, in tiles (Chebyshev) from the main choke center. */
  val chokeRingFarTiles: Int = 6

  /** Ring membership (pure tile distance): the cannon sits between five
   * and six tiles off the choke - close enough to cover it, constrained
   * enough that the placer cannot wander off to a random slot. */
  def inRing(chebyshevTiles: Int): Boolean =
    chebyshevTiles >= chokeRingNearTiles && chebyshevTiles <= chokeRingFarTiles

  /** Hall cannon offset, in tiles from the main hall toward the choke:
   * between the minerals and the choke, at the front of the base -
   * never at its back. */
  val hallFrontTiles: Double = 2.5

  /** Front anchor (pure pixels): the hall pushed toward the choke. */
  def frontAnchor(hall: Pixel, choke: Pixel, tiles: Double): Pixel =
    hall.project(choke, tiles * 32.0)

  /** Absolute-first Forge (pure flags): DT likely and no Forge started -
   * 150 minerals, every frame behind army pumps risks the game. */
  def urgentForgeNow(dtLikely: Boolean, haveForge: Boolean): Boolean =
    dtLikely && ! haveForge

  /** Our Citadel follows the Forge on a mirror race (pure flags): never
   * before the Forge exists - defense first, race second. */
  def urgentCitadelNow(dtLikely: Boolean, onDTPlan: Boolean, haveForge: Boolean, haveCitadel: Boolean): Boolean =
    dtLikely && onDTPlan && haveForge && ! haveCitadel

  /** Our Archives follows the Citadel on a mirror race (pure flags). */
  def urgentArchivesNow(dtLikely: Boolean, onDTPlan: Boolean, haveForge: Boolean, haveCitadel: Boolean, haveArchives: Boolean): Boolean =
    dtLikely && onDTPlan && haveForge && haveCitadel && ! haveArchives
}
