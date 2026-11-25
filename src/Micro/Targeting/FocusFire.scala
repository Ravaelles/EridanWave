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
  val minShooters: Int = 2

  /** Enough guns to make a shared target worth it: a pair deletes while a loner mills. */
  def enoughShooters(shooterCount: Int): Boolean =
    shooterCount >= minShooters

  /** Fewer reachable targets than this and the line spreads: two enemies
    * left still share one pick (weakest first), a single enemy is every
    * hull's pick by construction. */
  def enoughTargets(targetCount: Int): Boolean =
    targetCount >= minTargets

  /** Fewer reachable targets than this and there is no line to spread over. */
  val minTargets: Int = 2

  /** Melee cannot reach a shared target from its own slot; 3 tiles of range. */
  val minRangePixels: Int = 96

  /** At least this many shooters can hit the shared target, or there is no
    * concentration to win: a lone hitter is just its own pick. */
  val minReach: Int = 2

  def enoughReach(reach: Int): Boolean =
    reach >= minReach

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

  /**
   * Kill-ease volleys (pure numbers): time to delete the target given the
   * damage already pointed at it plus one average volley (mine, if I
   * join). A nearly-dead hull with one gun on it dies sooner than a
   * fresh hull with two - so the pack joins the finishing shot instead
   * of spreading over the line. Unanimous by construction: committed
   * damage and the squad average are the same numbers for every shooter.
   */
  def effectiveVolleys(healthWithShields: Double, committedDamage: Double, avgDamage: Double): Int =
    shotsToKill(healthWithShields, committedDamage + avgDamage)

  /** Widest shared reach first, then fewest volleys, then centroid distance
    * and id for stability. */
  def choose(candidates: Iterable[Candidate]): Option[Candidate] =
    Maff.minBy(candidates)(candidate => (-candidate.reach, candidate.shotsToKill, candidate.packDistanceTiles, candidate.id))

  /**
   * The squad's shared target for this attacker, if a mass fight is on and
   * the attacker could join in. None means "target normally". The committed
   * pick rides FocusMemory through reach churn (SharedFocus.hold): the pack
   * finishes instead of re-voting every frame. Entry keeps its quorum gates
   * untouched; only the hold is latched.
   */
  def sharedTarget(attacker: FriendlyUnitInfo): Option[UnitInfo] = {
    val squad = attacker.squad.getOrElse(return None)
    if ( ! attacker.unitClass.canAttack) return None
    if (attacker.unitClass.melee) return None
    if (attacker.pixelRangeMax < minRangePixels) return None
    if ( ! squad.engagedUpon && ! squad.engagingOn) return None

    val shooters = squad.attackers.filter(s => s.unitClass.canAttack && ! s.unitClass.melee)
    if ( ! enoughShooters(shooters.size)) return None

    val targets = squad.battleEnemies
      .filter(t => t.alive && ! t.unitClass.isBuilding)
      .filter(t => shooters.exists(s => s.canAttack(t) && s.inRangeToAttack(t)))
    if ( ! enoughTargets(targets.size)) return None

    val candidates = targets.map(t => (t, candidate(squad.centroidKey, t, shooters))).toSeq
    val bids = candidates.map { case (t, c) => SharedFocus.Bid(c.id, c.reach, c.shotsToKill) }
    val held = SharedFocus.hold(FocusMemory.read(squad).flatMap(id => bids.find(_.id == id)), bids)
    val chosen = held.flatMap(h => candidates.find(_._2.id == h.id).map(_._2))
      .orElse(choose(candidates.map(_._2)).filter(c => enoughReach(c.reach)))
    // Pack-level trace (one note per squad per frame): the latched id
    // stays put while hulls walk in and out of its reach - when hull
    // picks flip under a steady pack id, the splitter is the per-hull
    // reach filter in Target, not the vote.
    FocusMemory.noteOnce(squad, Lifecycle.With.frame) {
      Debugging.Probe.note("FocusPack", attacker.id, f"pack ${held.map(_.id).getOrElse(-1)} chose ${chosen.map(_.id).getOrElse(-1)}")
    }
    chosen match {
      case Some(c) =>
        FocusMemory.write(squad, c.id)
        held.foreach(h => {
          // Tuning signal, not an error: holding past a strictly cheaper
          // grind. Counts once per squad per frame; a rising count means
          // the hold band wants review, not that the hold misfires.
          if (bids.exists(b => b.id != h.id && b.costToKill < h.costToKill)) {
            FocusMemory.noteOnce(squad, Lifecycle.With.frame) {
              Debugging.ErrorLedger.note(
                "focusHeldPastBetter.at." + attacker.unitClass.toString,
                Debugging.ErrorLedger.Priority.Low,
                f"pack holds ${h.id} over a cheaper grind, frame ${Lifecycle.With.frame}")
            }
          }
        })

        candidates.find(_._2.id == c.id).map(_._1)
      case None =>
        FocusMemory.clear(squad)
        None
    }
  }

  private def candidate(
    pack     : Mathematics.Points.Pixel,
    target   : UnitInfo,
    shooters : Seq[UnitInfo]): Candidate = {
    val damages   = shooters.map(_.damageOnNextHitAgainst(target))
    val committed = shooters
      .filter(s => s.friendly.exists(_.agent.toAttack.exists(_.id == target.id)))
      .map(_.damageOnNextHitAgainst(target)).sum
    val avgDamage = if (shooters.nonEmpty) damages.sum / shooters.size else 0.0
    Candidate(
      shotsToKill      = effectiveVolleys(target.totalHealth, committed, avgDamage),
      reach            = shooters.count(s => s.canAttack(target) && s.inRangeToAttack(target)),
      packDistanceTiles = target.pixelDistanceCenter(pack) / 32.0,
      id               = target.id)
  }
}
