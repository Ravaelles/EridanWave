package Micro.Targeting

import Mathematics.Maff
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Focus fire for massed ranged armies: every shooter in the squad aims at one
 * shared target instead of each unit picking the enemy nearest to itself.
 * Individually sensible picks spread damage over the whole enemy line; a
 * concentrated volley deletes one unit at a time and is how the stock AI
 * beats an evenly matched pack.
 *
 * The shared target is the enemy the most shooters can already hit (that is
 * what makes the fire actually shared), tie-broken by the fewest squad
 * volleys to kill, then by distance to the squad centroid, then by id. Every
 * key is built from squad-wide numbers rather than the caller's own damage,
 * so all shooters compute the same answer by construction. Only a
 * real mass fight qualifies: enough ranged shooters, several targets in
 * reach, contact already on, and at least a handful able to hit the same
 * enemy. Buildings stay out of it - razing keeps its own targeting.
 *
 * Pure decisions on numbers, game-aware seam at the bottom.
 */
object FocusFire {

  /** Fewer shooters than this can't delete a unit in one volley. */
  val minShooters: Int = 4

  /** Fewer reachable targets than this and there is no line to spread over. */
  val minTargets: Int = 3

  /** Melee cannot reach a shared target from its own slot; 3 tiles of range. */
  val minRangePixels: Int = 96

  /** At least this many shooters can hit the shared target, or there is no
    * concentration to win. */
  val minReach: Int = 3

  /** One candidate target, reduced to the numbers the choice reads. */
  final case class Candidate(shotsToKill: Int, reach: Int, packDistanceTiles: Double, id: Int)

  /** Damage the whole firing line puts into one target per volley. */
  def volleyDamage(damages: Seq[Int]): Int = damages.sum

  /** Volleys the line needs for a target: the same number for every shooter,
    * because it prices the squad's combined damage, not one hull's. */
  def volleysToKill(healthWithShields: Double, damages: Seq[Int]): Int =
    shotsToKill(healthWithShields, volleyDamage(damages).toDouble)

  /** Volley count: hits needed to remove the target's remaining health. */
  def shotsToKill(healthWithShields: Double, damagePerShot: Double): Int = {
    if (damagePerShot <= 0) return Int.MaxValue

    Math.max(1, Math.ceil(healthWithShields / damagePerShot).toInt)
  }

  /** Widest shared reach first, then fewest volleys, then centroid distance
    * and id for stability. */
  def choose(candidates: Iterable[Candidate]): Option[Candidate] =
    Maff.minBy(candidates)(candidate => (-candidate.reach, candidate.shotsToKill, candidate.packDistanceTiles, candidate.id))

  /**
   * The squad's shared target for this attacker, if a mass fight is on and
   * the attacker could join in. None means "target normally".
   */
  def sharedTarget(attacker: FriendlyUnitInfo): Option[UnitInfo] = {
    val squad = attacker.squad.getOrElse(return None)
    if ( ! attacker.unitClass.canAttack) return None
    if (attacker.unitClass.melee) return None
    if (attacker.pixelRangeMax < minRangePixels) return None
    if ( ! squad.engagedUpon && ! squad.engagingOn) return None

    val shooters = squad.attackers.filter(s => s.unitClass.canAttack && ! s.unitClass.melee)
    if (shooters.size < minShooters) return None

    val targets = squad.battleEnemies
      .filter(t => t.alive && ! t.unitClass.isBuilding)
      .filter(t => shooters.exists(s => s.canAttack(t) && s.inRangeToAttack(t)))
    if (targets.size < minTargets) return None

    val candidates = targets.map(t => (t, candidate(squad.centroidKey, t, shooters)))
    choose(candidates.map(_._2))
      .filter(_.reach >= minReach)
      .flatMap(chosen => candidates.find(_._2.id == chosen.id).map(_._1))
  }

  private def candidate(
    pack     : Mathematics.Points.Pixel,
    target   : UnitInfo,
    shooters : Seq[UnitInfo]): Candidate =
    Candidate(
      shotsToKill      = volleysToKill(target.totalHealth, shooters.map(_.damageOnNextHitAgainst(target))),
      reach            = shooters.count(s => s.canAttack(target) && s.inRangeToAttack(target)),
      packDistanceTiles = target.pixelDistanceCenter(pack) / 32.0,
      id               = target.id)
}
