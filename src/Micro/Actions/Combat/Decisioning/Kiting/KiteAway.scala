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
 * about its targets. So after firing it pulls back while its weapon cools
 * down - waiting time it pays anyway - instead of standing in range posing.
 * Melee on top of us always kites while cooling (range abuse, not the duel);
 * against ranged guns only hulls below the wound line yield, everything
 * else holds its volleys: a scratch never outweighs a volley in an even
 * duel. Non-zero cooldown gates because the dragoon attack animation eats
 * frames either way. Dying hulls are also exempt from volley discipline:
 * holding fire while exposed is the worst outcome.
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

  /** Seriously wounded withdraw while cooldown exceeds this. */
  var seriousCooldown: Int = 10

  /** Critically wounded withdraw while cooldown exceeds this. */
  var criticalCooldown: Int = 4

  /** Mildly wounded withdraw while cooldown exceeds this. */
  var mildCooldown: Int = 18

  /** Retreat only while an enemy presses within firing range plus this margin (tiles). */
  var enemyMarginTiles: Double = 1.8

  /**
   * Fresh hulls never kite: at or below this much shield damage the
   * volley outweighs the yield, so the line holds and shoots instead
   * of donating DPS for distance. Owner order from the ZealDrag mirror.
   */
  var freshShieldWound: Int = 14

  /**
   * Melee contact radius, in tiles: kite only with blades this close.
   * Ranged-only pressure never triggers the kite - the plant holds the
   * loaded volleys, the approach owns the rest.
   */
  var meleeCloseTiles: Double = 2.6

  /** Zerg fresh hulls hold: the classic ladder decides the rest. */
  val zergShieldWound: Int = 8

  /**
   * Loaded window (owner order): a gun at or below this cooldown holds
   * its volley instead of yielding ground the return trip would eat.
   * Shoot every cycle - the kite only ever spends cooling frames.
   */
  var kiteCooldownFrames: Int = 10

  def holdsForVolley(cooldownLeft: Int): Boolean =
    cooldownLeft <= kiteCooldownFrames

  /**
   * Gun's edge, in tiles: already standing this close to max range
   * means any step out donates the next volley, so the hull holds its
   * ground instead of yielding it.
   */
  var kiteEdgeTiles: Double = 0.7

  def holdsTheEdge(distTiles: Double, rangeTiles: Double): Boolean =
    distTiles >= rangeTiles - kiteEdgeTiles

  /**
   * Fresh, loaded and clear hulls hold their fire instead of kiting:
   * with the gun inside its window, enough hull to trade, and nothing
   * inside contact range, the volley outweighs any ground the yield
   * would buy. Owner order from the ZealDrag mirror (the kite donated
   * the shooting).
   */
  var steadyCooldownFrames: Int = 15

  var steadyHullHp: Int = 80

  var steadyContactTiles: Double = 2.7

  def holdsFire(cooldownLeft: Int, hitPoints: Int, enemyClose: Boolean): Boolean =
    cooldownLeft <= steadyCooldownFrames && hitPoints >= steadyHullHp && ! enemyClose

  /**
   * Kite gate (pure numbers for the suite): wounded past fresh AND
   * melee in contact, or stand and shoot. Ranged-only pressure never
   * kites; the wound ladder below decides the cooling hulls that pass.
   */
  def shouldKite(shieldWound: Int, meleeNearby: Boolean): Boolean =
    shieldWound > freshShieldWound && meleeNearby

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

  /**
   * Contact floor (pure numbers for the suite): under blades the yield
   * takes the paced step over the wound want, never below it. A fresh
   * wound want is ~zero and a zero step is standing; the paced step
   * keeps the hull ahead of the chase while landing every volley.
   */
  def contactYield(woundWantPixels: Double, pacedPixels: Double): Double =
    Math.max(woundWantPixels, pacedPixels)

  /**
   * Edge-capped yield (pure numbers for the suite): the step never
   * walks past the gun's own reach minus the edge margin. A hull
   * already at the edge holds instead of donating ground it cannot
   * shoot back across.
   */
  def edgeCappedYield(stepPixels: Double, distTiles: Double, rangeTiles: Double, edgeTiles: Double): Double =
    Math.min(stepPixels, Math.max(0.0, rangeTiles - edgeTiles - distTiles) * 32.0)

  def shouldRetreat(
    totalHealth: Int,
    cooldownLeft: Int,
    enemyInMargin: Boolean,
    hasMeleeNearby: Boolean): Boolean = {
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
