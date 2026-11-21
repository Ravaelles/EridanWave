package Micro.Actions.Combat.Decisioning

import ProxyBwapi.UnitInfo.UnitInfo

/**
 * Tank hold (pure numbers, no game access): ranged ground warriors never
 * close a tank to fight it - even an unsieged gun outranges a Dragoon,
 * and a closing hull walks into the siege it was sent to answer. Hold
 * the line at the tank's reach instead and shoot what comes in reach;
 * inside the footprint the withdrawal owns the frame, past the band the
 * approach does. The point-blank hug stays with TankHug, which owns the
 * cooling duel it wins. Distances in tiles.
 */
object TankHold {

  /** Hold instead of closing on a tank this far past its reach, in tiles. */
  val holdBandTiles: Double = 2.0

  def isTank(target: UnitInfo): Boolean =
    target.unitClass.isTank

  def shouldHold(targetIsTank: Boolean, distTiles: Double, tankReachTiles: Double): Boolean =
    targetIsTank && distTiles >= tankReachTiles && distTiles <= tankReachTiles + holdBandTiles
}
