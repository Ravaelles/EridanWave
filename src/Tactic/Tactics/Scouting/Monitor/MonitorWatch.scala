package Tactic.Tactics.Scouting.Monitor

import Lifecycle.With
import Macro.Facts.MacroFacts
import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Tactics.Scouting.TacticMonitor
import Utilities.Time.Seconds
import Utilities.UnitFilters.IsMobileDetector

/**
 * Natural monitoring duty, plus its death memory: an eye eaten on this
 * duty grounds it while we are below three Observers.
 */
object MonitorWatch {

  case class WatchMemory(lastFrame: Int = -1, lastIds: Set[Int] = Set.empty, deathFrame: Int = -1)

  /**
   * @param memory previous memory (pass WatchMemory() on a fresh owner)
   * @param now current frame
   * @param alive is the unit id still alive
   * @param observerCount our Observers right now
   * @return (updated memory, grounded)
   */
  def refresh(
    memory        : WatchMemory,
    now           : Int,
    alive         : Int => Boolean,
    observerCount : Int): (WatchMemory, Boolean) = {
    val base = if (now < memory.lastFrame) WatchMemory(now) else memory.copy(lastFrame = now)
    val death = if (base.lastIds.exists(id => ! alive(id))) now else base.deathFrame

    (base.copy(deathFrame = death), death >= 0 && observerCount < 3)
  }

  def applies(): Boolean = {
    if ( ! With.blackboard.monitorBases()) return false
    if (With.units.existsEnemy(IsMobileDetector)) return false
    if (With.units.countEnemy(Terran.Factory) > 7) return false
    if (With.enemies.exists(_.hasTech(Terran.WraithCloak))) return false
    if (With.units.countOurs(Protoss.Observer) < 3 && MacroFacts.enemyHasShown(Terran.SpiderMine)) return false

    true
  }

  def act(monitor: TacticMonitor, eye: FriendlyUnitInfo): Unit = {
    val cloaked = eye.cloaked
    val naturals = With.geography.enemyBases.flatMap(_.natural.filter(b =>
      With.framesSince(b.lastFrameScoutedByUs) > Seconds(25)()))
    val bases = Maff.orElse(naturals, With.geography.enemyBases).toVector
    eye.intend(monitor)
      .setTerminus(With.geography.home.center)
      .setScout(bases.flatMap(_.zone.tiles.filter(t =>
        t.enemyRangeAir <= 0 && ( ! cloaked || ! t.enemyDetectedUnchecked))))
  }
}
