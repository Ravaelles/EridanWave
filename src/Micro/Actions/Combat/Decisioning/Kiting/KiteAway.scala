package Micro.Actions.Combat.Decisioning.Kiting

import Mathematics.Maff

/**
 * KiteAway: wounded units yield ground instead of dying in place.
 *
 * Knobs are public vars (Atlantis style): the tuning strategy assigns them
 * directly by name, and they stay readable and editable right here.
 *
 * A dragoon below ~60 total HP dies within a second to focused fire, and our
 * own TargetScoring (+4.0 * injury) proves the enemy thinks the same way
 * about its targets. So: the more wounded the unit, the deeper it sits, and
 * after firing it pulls back while its weapon cools down - waiting time it
 * pays anyway - instead of standing in range posing. Healthy units hold the
 * mean line (FormationApproach governs them); only wounded ones rotate.
 *
 * Two wound levels (total HP = hit points + shields, as in Atlantis):
 * seriously wounded retreat until almost ready to shoot again, critically
 * wounded until fully ready. Non-zero gates because the dragoon attack
 * animation eats frames either way. Seriously wounded units are also exempt
 * from volley discipline: holding fire while exposed is the worst outcome.
 *
 * Pure decisions on numbers so they stay unit-testable without a game;
 * Combat supplies the game-aware seam (health, cooldown, contact).
 */
object KiteAway {

  /** Seriously wounded at or below this total HP (shields + hit points). */
  var seriousWoundHp: Int = 65

  /** Critically wounded at or below this total HP (shields + hit points). */
  var criticalWoundHp: Int = 25

  /** Only units more wounded than this rotate; healthy ones hold the line. */
  var woundScopeFraction: Double = 0.05

  /** Seriously wounded withdraw while cooldown exceeds this. */
  var seriousCooldown: Int = 10

  /** Critically wounded withdraw while cooldown exceeds this. */
  var criticalCooldown: Int = 4

  /** Mildly wounded withdraw while cooldown exceeds this. */
  var mildCooldown: Int = 15

  /** Retreat only while an enemy presses within firing range plus this margin (tiles). */
  var enemyMarginTiles: Double = 1.8

  /** A fully wounded unit sits this far (tiles) behind its line slot. Applied linearly with injury. */
  var maxOffsetTiles: Double = 1.4

  def isSeriouslyWounded(totalHealth: Int): Boolean = totalHealth <= seriousWoundHp
  def isCriticallyWounded(totalHealth: Int): Boolean = totalHealth <= criticalWoundHp

  def woundOffsetTiles(injury01: Double): Double =
    Maff.clamp(injury01, 0.0, 1.0) * maxOffsetTiles

  /** Cooldown above which the unit withdraws. None means hold/fire. */
  def retreatAboveCooldown(totalHealth: Int, woundFraction: Double): Option[Int] = {
    if (isCriticallyWounded(totalHealth)) Some(criticalCooldown)
    else if (isSeriouslyWounded(totalHealth)) Some(seriousCooldown)
    else if (woundFraction > woundScopeFraction) Some(mildCooldown)
    else None
  }

  def shouldRetreat(totalHealth: Int, woundFraction: Double, cooldownLeft: Int, enemyInMargin: Boolean): Boolean =
    enemyInMargin && retreatAboveCooldown(totalHealth, woundFraction).exists(cooldownLeft > _)

  /** Wounded units never hold fire for the volley while exposed. */
  def exemptFromVolley(totalHealth: Int): Boolean = isSeriouslyWounded(totalHealth)
}
