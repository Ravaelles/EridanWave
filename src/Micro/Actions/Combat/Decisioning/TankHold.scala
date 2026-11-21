package Micro.Actions.Combat.Decisioning

import ProxyBwapi.UnitInfo.UnitInfo

/**
 * Tank hold (pure numbers, no game access): ranged ground warriors never
 * close a tank to fight it - even an unsieged gun outranges a Dragoon,
 * and a closing hull walks into the siege it was sent to answer. Two
 * holds, one rule: inside our own reach stand and shoot, outside the
 * tank's reach stand off at most two tiles past it and shoot what
 * comes in reach. Between our reach and the tank's (outranged under
 * fire) the hold stays out - the approach closes to reply range and
 * the withdrawal owns the footprint. The point-blank hug stays with
 * TankHug, which owns the cooling duel it wins. Distances in tiles.
 */
object TankHold {

  /** Hold instead of closing on a tank this far past its reach, in tiles. */
  val holdBandTiles: Double = 2.0

  def isTank(target: UnitInfo): Boolean =
    target.unitClass.isTank

  def shouldHold(targetIsTank: Boolean, distTiles: Double, ownRangeTiles: Double, tankReachTiles: Double): Boolean =
    targetIsTank && (distTiles <= ownRangeTiles ||
      (distTiles >= tankReachTiles && distTiles <= tankReachTiles + holdBandTiles))
}
