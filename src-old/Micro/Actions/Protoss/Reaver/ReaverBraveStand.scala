package Micro.Actions.Protoss.Reaver

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

  def brave(shields: Int, framesSinceFired: Int): Boolean =
    shields >= braveShields && framesSinceFired >= staleShotFrames

  def applies(situation: ReaverSituation): Boolean =
    ! situation.loaded &&
      ! situation.committed &&
      ! situation.doomed &&
      ! situation.targetedByScarab &&
      ! situation.staticInFace &&
      brave(situation.shields, situation.framesSinceFired) &&
      ReaverFireDiscipline.shotReady(situation.scarabs, situation.hasActiveScarab, situation.cooling) &&
      situation.foeInReach &&
      ! situation.tankCovered &&
      ! situation.pressuredClose

  def act(unit: FriendlyUnitInfo): Unit = {
    ReaverActions.holdGround(unit, "BraveStand")
  }
}
