package Information.Scouting

import Lifecycle.With
import Performance.Cache
import Utilities.UnitFilters.IsWorker
import ProxyBwapi.Races.Zerg

trait EnemyScouting {
  def enemyHasScoutedUsWithWorker: Boolean = _enemyHasScoutedUsWithWorker
  // Baseless maps (UMS micro duels) have no metro and no scouts to eject:
  // answer empty instead of crashing ourMain on an empty base list.
  val zonesToLookForEnemyScouts = new Cache(() =>
    if (With.geography.ourBases.isEmpty) Vector.empty else With.geography.ourMetro.zones)
  val enemyScouts = new Cache(() => With.units.enemy.filter(u => u.isAny(Zerg.Overlord, IsWorker) && u.likelyStillThere && zonesToLookForEnemyScouts().contains(u.zone)))

  private var _enemyHasScoutedUsWithWorker = false

  protected def updateEnemyScouting(): Unit = {
    _enemyHasScoutedUsWithWorker ||= With.geography.ourBases.exists(_.enemies.exists(IsWorker))
  }
}
