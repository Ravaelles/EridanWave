package Micro.Actions.Combat.Decisioning

import ProxyBwapi.UnitInfo.UnitInfo

/**
 * Tank hold (pure numbers, no game access): ranged ground warriors never
 * close a tank to fight it - even an unsieged gun outranges a Dragoon,
 * and a closing hull walks into the siege it was sent to answer. Hold
 * the line instead and shoot what comes in reach; the point-blank hug
 * stays with TankHug, which owns the cooling duel it wins. Distances in
 * tiles beyond our own max range.
 */
object TankHold {

  /** Hold instead of closing on a tank this far past our max range, in tiles. */
  val holdBandTiles: Double = 2.0

  def isTank(target: UnitInfo): Boolean =
    target.unitClass.isTank

  def shouldHold(targetIsTank: Boolean, distBeyondRangeTiles: Double): Boolean =
    targetIsTank && distBeyondRangeTiles <= holdBandTiles
}
