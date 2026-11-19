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
}
