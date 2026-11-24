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

  /**
   * Absolute contact radius, in tiles (owner order): blades this close
   * always outrun a cooling gun - no volley to hold, no trade to take.
   */
  var contactKiteTiles: Double = 2.0

  /**
   * Absolute contact cooldown, in frames (owner order): at this much
   * cooldown left the gun holds no volley worth standing for.
   */
  var contactKiteCooldown: Int = 15

  /**
   * Absolute contact rule (pure flags for the suite): a cooling dragoon
   * with blades on top always yields - no volley to hold, no trade to
   * take, no gate to consult. Loaded guns never reach it (their
   * cooldown sits below the bar by construction).
   */
  def mustFleeContact(isDragoon: Boolean, cooldownLeft: Int, meleeContact: Boolean): Boolean =
    isDragoon && meleeContact && cooldownLeft >= contactKiteCooldown

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

  /**
   * Chase radius, in tiles: an approaching blade inside this starts the
   * kite before contact - from first sight, so the dance never walks
   * the approach back into the chaser. Melee never outranges the volley,
   * so a fresh hull yields to the chase just like a wounded one - waiting
   * for the first hit donates the very contact the kite exists to avoid.
   */
  var chaseTiles: Double = 7.0

  def shouldChase(distTiles: Double, chaseTiles: Double): Boolean =
    distTiles <= chaseTiles

  /**
   * Leash radius, in tiles: a kiter farther than this from its nearest
   * friendly warrior steers back toward the pack instead of across the
   * map - but never closer to the chaser than it stands, and never on
   * a heading that turns it back into the chase. No warriors out, no
   * leash: a lone hull kites free.
   */
  var leashTiles: Double = 6.0

  def shouldLeash(distFriendTiles: Double, leashTiles: Double, distFriendFoePx: Double, distUnitFoePx: Double, alignedAway: Boolean): Boolean =
    distFriendTiles > leashTiles && distFriendFoePx > distUnitFoePx && alignedAway

  /**
   * Away-hemisphere check (pure pixels for the suite): the run home only
   * counts when heading for the friend also heads away from the foe.
   * Turning back into the chase to rejoin trades the duel for the pack.
   */
  def alignedAway(unitPx: Mathematics.Points.Pixel, friendPx: Mathematics.Points.Pixel, foePx: Mathematics.Points.Pixel): Boolean =
    (friendPx.x - unitPx.x) * (unitPx.x - foePx.x) + (friendPx.y - unitPx.y) * (unitPx.y - foePx.y) > 0

  /**
   * Escape floor, in tiles: a loaded hull with blades on top still steps
   * out instead of planting into the swing. The gap reopens, the next
   * volley fires from range - one donated shot beats every swing taken
   * standing in contact.
   */
  var meleeEscapeTiles: Double = 1.0

  /**
   * Committed stride, in tiles: short hops brake into arrival every few
   * frames and never reach top speed, so a lone-blade kite strides past
   * its point instead of stepping to it. The stride only ever grows the
   * yield, never shrinks it - the direction still comes from the duel.
   */
  var kiteCommitTiles: Double = 6.0

  /**
   * Lone-blade duel (pure flags for the suite): with no guns nearby a
   * chased ranged hull belongs to the duel before contact, never to
   * the line. One read shared by the Close outer gate and the
   * formation decline, so vote and leg agree by construction.
   */
  def loneBladeDuel(gunsNear: Boolean, meleeChase: Boolean, melee: Boolean): Boolean =
    ! gunsNear && meleeChase && ! melee

  def escapeYield(stepPixels: Double, escapeTiles: Double): Double =
    Math.max(stepPixels, escapeTiles * 32.0)

  /**
   * Heading hold, in frames: re-deriving the course every frame from a
   * jittering chaser turns forever instead of cruising, so a held
   * heading stands until it lapses or the duel turns past the cone.
   * New games restart the frame counter, which releases every hold.
   */
  var steadyHoldFrames: Int = 8

  def shouldRefreshHeading(lastFrame: Int, frame: Int): Boolean =
    frame < lastFrame || frame - lastFrame >= steadyHoldFrames

  def holdsHeading(freshRadians: Double, heldRadians: Double, coneRadians: Double): Boolean =
    Math.abs(Maff.normalizePiToPi(freshRadians - heldRadians)) <= coneRadians

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
