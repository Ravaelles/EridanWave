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

  /**
   * Kite away from ranged guns only below this total HP: a mild wound
   * never outweighs a volley in an even duel, so the line holds and
   * shoots until the hull is one volley from death - then it gets out
   * instead of donating itself. Melee on top still kites while cooling
   * (range abuse, not the volley race).
   */
  var kiteVsRangedHp: Int = 61

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

  /** A banked mineral stock this big buys the trade: skip kiting, keep DPS up. */
  var richBankMinerals: Int = 1000

  /** Rich enough that the unit is expendable: hold and shoot, never yield ground. */
  def skipKiting(minerals: Int): Boolean = minerals >= richBankMinerals

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

  /** Fresh hulls never yield ground, in hit points: a scratch never outweighs a volley. */
  val freshWoundHp: Int = 14

  /** Healthy hulls hold with this many shields or more: they can pay for the trade. */
  val healthyShieldHp: Int = 30

  /** Above the pack median: the beating spreads instead of piling onto one hull. */
  def aboveMedian(totalHp: Int, medianHp: Int): Boolean = totalHp > medianHp

  def shouldRetreat(
    totalHealth: Int,
    woundFraction: Double,
    cooldownLeft: Int,
    enemyInMargin: Boolean,
    hasMeleeNearby: Boolean,
    woundPoints: Int,
    shields: Int,
    medianHp: Int): Boolean = {
    if ( ! enemyInMargin) return false
    // Melee on top of us always kites while cooling: standing trades
    // volleys for contact we cannot win, so the wound ladder is skipped.
    if (hasMeleeNearby) return cooldownLeft > mildCooldown
    // Against ranged guns only the dying yield: the line holds its
    // volleys until the hull is nearly gone, then it saves itself.
    if (isCriticallyWounded(totalHealth)) return cooldownLeft > criticalCooldown
    if (totalHealth < kiteVsRangedHp) return cooldownLeft > seriousCooldown

    false
  }

  /** Wounded units never hold fire for the volley while exposed. */
  def exemptFromVolley(totalHealth: Int): Boolean = isSeriouslyWounded(totalHealth)

  /**
   * Masonry never chases: kiting away from a building anchor donates DPS
   * and time for nothing - a static cannot pursue, so the walk buys no
   * safety. The behavior checks this past hold-to-shoot: a free shot at
   * a pressing chaser is still taken.
   */
  def holdAgainstBuildings(anchorIsBuilding: Boolean): Boolean = anchorIsBuilding
}
