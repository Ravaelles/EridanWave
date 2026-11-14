package Debugging

import Lifecycle.With

/**
 * UMS map snapshot dumper (development only, never in a shipped game):
 * with ERIDAN_DUMP_SNAPSHOT=1 in the environment, logs every unit on
 * the map once, at the first frame that sees any. Statics only -
 * position, side and type, never HP, orders or cooldowns: snapshots
 * seed StarEngine scenarios, which must create the fight by playing,
 * not replay it. Mirrors the Atlantis MAP_SNAPSHOT=1 dumper idea.
 */
object MapSnapshot {

  private var dumped: Boolean = false

  def reset(): Unit = { dumped = false }

  def maybeDump(): Unit = {
    if (dumped) return
    if (sys.env.get("ERIDAN_DUMP_SNAPSHOT").forall(_ != "1")) return

    val units = With.units.all.toVector
    if (units.isEmpty) return

    dumped = true
    units.foreach(u => {
      val side = if (u.isFriendly) "ours" else if (u.isEnemy) "foe" else "neutral"
      With.logger.debug(f"SNAPSHOT $side%-8s id=${u.id}%4d bwapi=${u.bwapiUnit.getType}%-28s class=${u.unitClass}%-28s x=${u.pixel.x}%5d y=${u.pixel.y}%5d")
    })
  }
}
