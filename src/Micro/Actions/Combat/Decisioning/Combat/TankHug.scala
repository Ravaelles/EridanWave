package Micro.Actions.Combat.Decisioning

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

  /**
   * Hug while the weapon still needs this many frames: closing takes
   * longer than the swing, so a nearly-ready gun holds its shot
   * instead of walking through the aim - below this the hull shoots
   * first, then walks on the cooldown.
   */
  val coolingFrames: Int = 5

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
}
