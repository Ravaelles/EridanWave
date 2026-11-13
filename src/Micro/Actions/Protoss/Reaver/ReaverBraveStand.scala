package Micro.Actions.Protoss.Reaver

import Micro.Agency.Commander
import ProxyBwapi.UnitInfo.FriendlyUnitInfo

/**
 * Brave stand: a shielded Reaver that has not fired for seconds holds its
 * ground and shoots instead of hopping - boarding first would circle
 * without dealing damage. Sieged tanks veto everything: their range plus
 * margin is never entered. Statics and incoming Scarabs keep their own
 * rules later in the chain.
 */
object ReaverBraveStand {

  /** Stand instead of boarding above this many shields. */
  val braveShields: Int = 50

  /** A shot this stale, in frames, means the Reaver idles instead of dueling. */
  val staleShotFrames: Int = 4 * 24

  /** Enemies this close, in tiles, are engaged instead of fled. */
  val engageTiles: Double = 8.5

  /** Sieged-tank range plus this margin, in tiles, is never entered. */
  val tankMarginTiles: Double = 1.0

  def brave(shields: Int, framesSinceFired: Int): Boolean =
    shields >= braveShields && framesSinceFired >= staleShotFrames

  def tankCovered(tankRangeTiles: Double, distTiles: Double): Boolean =
    distTiles <= tankRangeTiles + tankMarginTiles

  def armed(scarabs: Int, hasActiveScarab: Boolean): Boolean =
    scarabs > 0 || hasActiveScarab

  def applies(situation: ReaverSituation): Boolean =
    ! situation.loaded &&
      ! situation.committed &&
      ! situation.doomed &&
      ! situation.targetedByScarab &&
      ! situation.staticInFace &&
      brave(situation.shields, situation.framesSinceFired) &&
      armed(situation.scarabs, situation.hasActiveScarab) &&
      situation.foeInReach &&
      ! situation.tankCovered

  def act(unit: FriendlyUnitInfo): Unit = {
    unit.agent.act("BraveStand")
    Commander.hold(unit)
  }
}
