package Micro.Actions.Combat.Decisioning

import Utilities.Time.Seconds

/**
 * Pure siege-hug decision (no game access): a ground warrior with a hot
 * gun walks onto the nearest sieged tank instead of kiting around it.
 * Cooling down is waiting time paid anyway, and at touching distance the
 * tank cannot answer - so hold the pose, walk in, shoot point-blank.
 * Only for units that want to fight; routing units never hug.
 * Distances in tiles.
 */
object TankHug {

  /** Hug a sieged tank inside this radius, in tiles. */
  val siegeRadiusTiles: Double = 7.0

  /** Close to at most this gap, in tiles, then shoot point-blank. */
  val hugGapTiles: Double = 0.2

  /** Hug while the weapon still needs this many frames. */
  val coolingFrames: Int = 8

  /** Shells landing this recently still count as incoming fire, in frames. */
  val shellShockFrames: Int = Seconds(2)()

  /**
   * Melee stand, in tiles: a blade this close to a sieged tank never
   * withdraws - the tank cannot answer point-blank, so leaving trades
   * a free kill for a walk back into the shells.
   */
  val meleeStandTiles: Double = 1.1

  def holdsTank(distTiles: Double, isMelee: Boolean): Boolean =
    isMelee && distTiles <= meleeStandTiles

  def shouldHug(
    siegedTankTiles: Double,
    cooldownLeft: Int,
    shouldFight: Boolean,
    canMove: Boolean): Boolean =
    shouldFight && canMove && siegedTankTiles <= siegeRadiusTiles && cooldownLeft >= coolingFrames

  def closeEnough(distTiles: Double): Boolean =
    distTiles <= hugGapTiles

  /**
   * Shell shock (no game access): a hull taking siege fire outside hugging
   * range breaks off instead of trading volleys it cannot win. The latch
   * on the retreat (ReengageLatch) decides when it may come back; this
   * only starts the withdrawal, on the first shells, never inside the
   * hug where the tank cannot answer.
   */
  def shouldWithdraw(shelledRecently: Boolean, nearestSiegedTiles: Double, isMelee: Boolean): Boolean =
    shelledRecently && nearestSiegedTiles > hugGapTiles && ! holdsTank(nearestSiegedTiles, isMelee)

  /**
   * Siege footprint, in tiles: a sieged tank outranges every ground
   * warrior, so standing (or closing) inside this trades free shells
   * for nothing. Siege range is twelve; the half tile is the margin.
   */
  val siegeTooCloseTiles: Double = 12.5

  /**
   * Siege release, in tiles: the leave latches until clearly past the
   * footprint, so the edge never jitters the technique every few
   * frames - entry and release ship together.
   */
  val siegeReleaseTiles: Double = 14.5

  def tooCloseToSiege(siegedTankTiles: Double): Boolean =
    siegedTankTiles < siegeTooCloseTiles

  /**
   * Proactive siege withdrawal (pure numbers for the suite): a ranged
   * fighter with a sieged tank inside the footprint walks out before
   * the first shell lands instead of holding under it - the reactive
   * shell-shock above only fires after the guns already spoke. The
   * cooling hug still owns the point-blank duel it wins (walk in and
   * shoot where the tank cannot answer); routers, the immobile and
   * blades keep their own rules, so only ranged fighters yield. The
   * ReengageLatch on the retreat owns the release past the guns.
   */
  def shouldLeaveSiege(
    siegedTankTiles: Double,
    cooldownLeft: Int,
    shouldFight: Boolean,
    canMove: Boolean,
    isMelee: Boolean): Boolean =
    shouldFight && canMove && ! isMelee && tooCloseToSiege(siegedTankTiles) &&
      ! shouldHug(siegedTankTiles, cooldownLeft, shouldFight, canMove)
}
