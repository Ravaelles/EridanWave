package Lifecycle

import Lifecycle.Launch.BwapiIni
import Mathematics.Maff
import Mathematics.Points.Pixel
import ProxyBwapi.UnitInfo.{ForeignUnitInfo, FriendlyUnitInfo}

/**
 * UMS (micro/scenario map) detection, mirroring Atlantis' OnGameStarted check.
 *
 * A standard melee opening gives us workers and a base. Finding at most a
 * couple of workers - or no town hall at all - means this is a custom scenario
 * map, where macro build orders make no sense and mostly throw. On detection:
 *   1. [[With.umsMode]] is set so gameplans/strategies can branch on it;
 *   2. the live bwapi.ini is patched to game_type=USE_MAP_SETTINGS, so the
 *      NEXT game on this map starts with the right type (bwapi.ini is read once
 *      at game start, so the current game keeps whatever type it started with);
 *   3. every frame each squadless warrior gets an explicit terminus toward the
 *      enemy, so units march and fight instead of standing still.
 */
object UmsMode {

  def detectAndApply(): Unit = {
    With.umsMode = false
    MapSpecificCommander.reset()
    try {
      detectAndApplyInner()
    } catch {
      case e: Exception =>
        System.out.println("[launcher] UMS detection failed (assuming melee): " + e)
    }
  }

  private def detectAndApplyInner(): Unit = {
    if (With.game == null || With.units == null) return
    if (MapSpecificCommander.shouldTreatAsNormalMap()) return

    val workers = With.units.ours.count(_.unitClass.isWorker)
    val bases   = With.units.ours.count(_.unitClass.isTownHall)
    System.out.println(f"[launcher] UMS check: $workers workers, $bases bases on ${With.mapFileName}")
    if (workers > 3 && bases > 0) return

    With.umsMode = true
    val message = f"### UMS mode detected: $workers workers, $bases bases on ${With.mapFileName} ###"
    System.out.println(message)
    With.logger.debug(message)

    // Persist the correction for the next game; no-op when already correct.
    val ini = BwapiIni.defaultLiveIni()
    if (ini.isFile) {
      BwapiIni.patch(ini, null, null, null, "USE_MAP_SETTINGS", false)
    } else {
      With.logger.debug(f"Skipping bwapi.ini game_type patch; not found at ${ini.getPath}")
    }
  }

  /**
   * Force UMS warriors toward the enemy; call every frame (cheap loop).
   * With no squads and no objectives a combat unit's destination defaults to
   * where it stands, so it never moves and only auto-retaliates. An explicit
   * terminus (nearest known enemy, else enemy start, else map center) makes it
   * march; Combat's in-frame decisions outrank the terminus, so targeting,
   * kiting and retreating keep working as designed.
   */
  def onFrame(): Unit = {
    if ( ! With.umsMode || With.game == null || With.units == null) return
    val enemies = With.units.enemy.toVector
    With.units.ours.foreach(unit => {
      if (unit.canAttack
        && unit.canMove
        && ! unit.unitClass.isWorker
        && unit.squad.isEmpty) {
        destinationFor(unit, enemies).foreach(unit.intent.setTerminus)
      }
    })
  }

  private def destinationFor(unit: FriendlyUnitInfo, enemies: Vector[ForeignUnitInfo]): Option[Pixel] = {
    if (enemies.nonEmpty) {
      Maff.minBy(enemies)((e: ForeignUnitInfo) => unit.pixelDistanceCenter(e.pixel)).map(_.pixel)
    } else {
      lazyEnemyStart().orElse(Some(Pixel(With.mapPixelWidth / 2, With.mapPixelHeight / 2)))
    }
  }

  private var cachedEnemyStart: Option[Pixel] = None
  private var cachedEnemyStartFrame: Int = -1

  private def lazyEnemyStart(): Option[Pixel] = {
    if (cachedEnemyStartFrame != With.frame && With.enemy != null) {
      cachedEnemyStartFrame = With.frame
      try {
        cachedEnemyStart = Some(With.enemy.startTile.center)
      } catch {
        case _: Exception => cachedEnemyStart = None
      }
    }
    cachedEnemyStart
  }
}
