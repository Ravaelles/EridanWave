package Micro.Actions.Combat.Decisioning.Skirmish

/**
 * Proxy defense (pure numbers, owner order): with few hulls home and an
 * enemy ground army already inside our natural-choke radius, the line
 * fights at the wall instead of routing - a retreat here donates the
 * main. PvP only: other matchups keep their own retreat rules. Ground
 * tiles throughout: the walk the enemy already made, not the crow flies.
 */
object ProxyDefense {

  /** Few hulls means this many complete warriors at most. */
  val maxDefenders: Int = 15

  def holdsGround(enemyIsProtoss: Boolean, ourWarriors: Int, nearestFoeMainTiles: Double, homeRadiusTiles: Double): Boolean =
    enemyIsProtoss && ourWarriors <= maxDefenders && nearestFoeMainTiles < homeRadiusTiles
}
