package Tactic.Corsair

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Tactic.Squads.support.SquadAcePilots
import Utilities.Latch

/**
 * Mid-duel Overlord dance: a cooling Corsair holds at standoff instead of
 * closing into the Spore math behind the Overlord; ours shoots the moment
 * it is ready. Close (or already firing) Corsairs go for the kill.
 * Without spore cover ahead there is nothing to hold against - the dance
 * yields and the hunt runs the Overlord down instead of watching it
 * leave, which is exactly what a standing Corsair does.
 */
object CorsairDanceToOverlord {

  /** Enemy static air defense this close to the Overlord covers it, in tiles. */
  val sporeCoverTiles: Double = 9.0

  /**
   * Latched standoff (auto-registered, reset per game): entering holds
   * through the cooldown cycle instead of flapping with the hunt every
   * few frames when the duel sits on the dance edge - each flip
   * re-issues orders and the re-aiming never lets the hull close, so
   * the Overlord walks away from a standing Corsair. Release when the
   * gun is ready (go kill) or the duel is over (go hunt).
   */
  private val dancing = new Latch

  /**
   * Dance only on a long cooldown: a Corsair with ten or fewer frames
   * left goes for the kill instead of holding standoff - the shot lands
   * before the Spore math matters.
   */
  val DanceCooldown = 11

  def shouldDance(cooldownLeft: Int, distTiles: Double, wound01: Double, sporeCover: Boolean): Boolean =
    sporeCover && cooldownLeft >= DanceCooldown && distTiles >= 3.0 + wound01 * 2.0

  /**
   * A duel is on: cooling down from a shot, or started one recently.
   * Without a shot there is no standoff to hold - claiming here strands
   * an idle Corsair instead of letting it hunt.
   */
  def dueling(cooldownLeft: Int, framesSinceAttack: Int): Boolean =
    cooldownLeft > 0 || framesSinceAttack <= 3 * 24

  /**
   * Ongoing attack, never interrupted: the Corsair already fights a live
   * Overlord in range, so the dance yields and the hunt keeps it. Pure
   * flags for the suite; the caller reads the live target.
   */
  def keepsAttacking(hasCommittedTarget: Boolean, targetAlive: Boolean, inRange: Boolean): Boolean =
    hasCommittedTarget && targetAlive && inRange

  def duelOverlord(squad: SquadAcePilots, unit: FriendlyUnitInfo): Option[UnitInfo] = {
    val assigned = squad.targets.toSeq.flatten.filter(o => Zerg.Overlord(o))
    Maff.minBy(assigned)(_.pixelDistanceSquared(unit.pixel))
      .orElse(unit.matchups.targetNearest.filter(o => Zerg.Overlord(o)))
  }

  def act(squad: SquadAcePilots, unit: FriendlyUnitInfo): Boolean = {
    if ( ! Protoss.Corsair(unit)) return false

    if ( ! dueling(unit.cooldownLeft, With.framesSince(unit.lastFrameStartingAttack))) return false

    duelOverlord(squad, unit).exists(overlord => {
      val committed = unit.agent.toAttack.filter(Zerg.Overlord(_))
      if (keepsAttacking(committed.nonEmpty, committed.exists(_.alive), committed.exists(t => unit.inRangeToAttack(t)))) return false

      val distTiles = unit.tileDistanceCenter(overlord.pixel)
      val sporeCover = unit.matchups.enemies.exists(e =>
        e.alive && e.unitClass.isStaticDefense && e.canAttackAir && e.pixelDistanceCenter(overlord.pixel) < sporeCoverTiles * 32.0)
      val duelingNow = dueling(unit.cooldownLeft, With.framesSince(unit.lastFrameStartingAttack))
      val dance = dancing(
        unit.id,
        fires    = duelingNow && shouldDance(unit.cooldownLeft, distTiles, unit.woundPercent(), sporeCover),
        releases = unit.cooldownLeft == 0 || ! duelingNow)
      if ( ! dance) return false

      if (shouldDance(unit.cooldownLeft, distTiles, unit.woundPercent(), sporeCover)) {
        unit.intend(squad).setTargets(Seq(overlord)).setTerminus(unit.pixel)
      } else {
        unit.intend(squad).setTargets(Seq(overlord)).setTerminus(overlord.pixel)
      }
      unit.agent.act("CorsairDance")
      true
    })
  }
}
