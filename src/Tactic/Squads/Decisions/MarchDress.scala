package Tactic.Squads.Decisions

import Utilities.Latch

/**
 * March dressing: leaders wait for the pack before first contact.
 *
 * FormationApproach dresses the line once the enemy is known, but at game
 * start nobody has seen anything yet, so the whole squad used to sprint at
 * full speed and arrive as a trickle. Instead compare each unit's distance to
 * the march goal against the pack mean: a unit running ahead of the mean
 * holds position until the pack catches up. Self-regulating (the mean moves
 * with the pack, so nobody waits long) and deadlock-free (units within
 * tolerance always march). SquadRally gathers radial stragglers; this holds
 * axial leaders. Pure decision so it stays unit-testable without a game.
 */
object MarchDress {

  /** Leaders this far ahead of the pack mean (tiles) wait instead of running on. */
  val waitAheadTiles: Double = 2.0

  /**
   * Latched dress wait (LoneMarch shape): the pack mean breathes a tile
   * or two every frame, so a sharp two-tile gate holds and releases the
   * same leader every few frames - a live UMS game showed Engage/Dress
   * alternating every other frame for eighty frames. Latch on past 2.5,
   * march on below 1.5, hold the side inside the band. Per-unit state,
   * reset per game like the other march watches.
   */
  val dressEnterTiles: Double = 2.5
  val dressExitTiles: Double = 1.5

  private val dressing = new Latch

  def reset(): Unit = { dressing.reset(); pushing.reset() }

  def holdDress(unitId: Int, aheadTiles: Double): Boolean =
    dressing(
      unitId,
      fires    = aheadTiles > dressEnterTiles,
      releases = aheadTiles < dressExitTiles,
      initial  = aheadTiles > waitAheadTiles)

  /** No dressing without contact: enemies this close, in tiles, justify it. */
  val dressEnemyRadiusTiles: Double = 20.0

  def enemyNear(distTiles: Double): Boolean =
    distTiles <= dressEnemyRadiusTiles

  /**
   * The dress wait itself (pure flags for the suite): leaders hold for
   * the pack only while the squad fights, contact is near, nothing
   * skips the wait, and - never while the hull has a shot right now.
   * A Dragoon holding its slot instead of firing the Vulture in reach
   * mills the whole column: the shot outranks the line, always.
   */
  def shouldHoldForPack(
    fightConsensus: Boolean,
    enemyNear: Boolean,
    skipCompactStrong: Boolean,
    staticNear: Boolean,
    massed: Boolean,
    holdDress: Boolean,
    targetsInRange: Boolean): Boolean =
    fightConsensus &&
      enemyNear &&
      ! skipCompactStrong &&
      ! staticNear &&
      ! massed &&
      holdDress &&
      ! targetsInRange

  /** A pack this strong marches through instead of dressing, in value. */
  val strongArmyStrength: Double = 150.0

  /** A pack this compact marches through instead of dressing, in tiles. */
  val compactAllyTiles: Double = 3.0

  /**
   * Compact-strong-unopposed skip (pure numbers for the suite): with no
   * armed enemy in sight, a strong pack marching elbow-to-elbow never
   * dresses - waiting only accordion-marches a formation nobody threatens.
   * The caller feeds live visibility, strength and spacing; this only
   * combines them.
   */
  def shouldSkipWait(strength: Double, nearestAllyTiles: Double, armedEnemyVisible: Boolean): Boolean =
    ! armedEnemyVisible && strength >= strongArmyStrength && nearestAllyTiles <= compactAllyTiles

  /** A crowd this big never waits for a line that already stands. */
  val massedFriendlies: Int = 6
  /** The radius that counts as company, in tiles. */
  val massedTiles: Double = 4.0

  /**
   * Massed skip (pure count for the suite): with this many friendlies
   * inside the company radius the line is already formed, so a leader
   * marches instead of posing for stragglers that are not straggling.
   * The caller counts live bodies inside massedTiles; this only draws
   * the line.
   */
  def isMassed(nearbyFriendlies: Int): Boolean =
    nearbyFriendlies >= massedFriendlies

  /** Never dress under static guns, in tiles: a leader holding inside their reach trades the line for nothing. */
  val staticDressTiles: Double = 8.0

  def staticNear(distTiles: Double): Boolean =
    distTiles <= staticDressTiles

  /** Never rally or slot under static guns, in tiles: a pack milling in their reach trades the line for nothing. */
  val staticPushTiles: Double = 10.0

  def shouldPushPastStatics(distTiles: Double): Boolean =
    distTiles <= staticPushTiles

  /**
   * Latched static push (LoneMarch shape): the gate below flips every
   * few frames for a unit breathing on exactly ten tiles - a live log
   * showed StaticPush alternating with Rallying/Marching/Returning
   * until the zealot died milling instead of attacking. Enter at ten,
   * rejoin the legs past twelve, hold the side inside the band.
   * Per-unit state, reset per game like the other march watches.
   */
  val staticPushExitTiles: Double = 12.0

  private val pushing = new Latch

  def pushPastStatics(unitId: Int, nearestTiles: Double): Boolean =
    pushing(
      unitId,
      fires    = nearestTiles <= staticPushTiles,
      releases = nearestTiles > staticPushExitTiles,
      initial  = shouldPushPastStatics(nearestTiles))
}
