package Tactic.Tactics.Scouting

import Tactic.Tactics.Tactic
import Tactic.Tactics.Scouting.Monitor.{MonitorEscort, MonitorReveal, MonitorWatch}

import Lifecycle.With
import Macro.Facts.MacroFacts
import Planning.ResourceLocks.LockUnits
import ProxyBwapi.Races.{Protoss, Zerg}
import Utilities.UnitCounters.CountUpTo
import Utilities.UnitFilters.IsAny
import Utilities.UnitPreferences.PreferClose

class TacticMonitor extends Tactic {

  // One eye pool, assigned in priority order each frame: army escort,
  // expo reveal, natural monitoring. A single lock keeps the priority in
  // exactly one method (launch) instead of scattered across duties.
  // CountUpTo(3): one seat per duty; spare eyes stay free.
  val eyes: LockUnits = new LockUnits(this, IsAny(Protoss.Observer, Zerg.Overlord), CountUpTo(3), interruptable = false)

  private var watchMemory = MonitorWatch.WatchMemory()

  override def launch(): Unit = {
    eyes
      .setPreference(PreferClose(With.geography.home.center))
      .acquire()
    val free = eyes.units.toVector.sortBy(_.id)
    if (With.geography.enemyBases.isEmpty) {
      // UMS: no bases to monitor or reveal; escort keeps the eye with
      // the muscle (Stardust ObserverActivity.EscortingArmy).
      free.headOption.foreach(eye => MonitorEscort.act(this, eye))

      return
    }

    // 1. Army escort has first claim: an eye overhead once burrowed
    // threats are known while the attack squad is out (the wipe case).
    val escort =
      if (MonitorEscort.applies(With.tactics.attackSquad.units.nonEmpty, MacroFacts.enemyHasShown(Zerg.Lurker, Zerg.LurkerEgg)))
        MonitorEscort.pick(free)
      else None
    escort.foreach(eye => MonitorEscort.act(this, eye))

    // 2. Expo reveal: a spare eye, or the first eye when safe.
    val rest = free.filterNot(escort.contains)
    val reveal = MonitorReveal.pick(rest, escort.isDefined)
    reveal.foreach { case (eye, site) => MonitorReveal.act(this, eye, site) }
    val remaining = rest.filterNot(reveal.map(_._1).contains)

    // 3. Natural monitoring takes the leftovers, unless grounded.
    val alive = (id: Int) => With.units.ours.exists(_.id == id)
    val (memory, grounded) = MonitorWatch.refresh(
      watchMemory, With.frame, alive, With.units.countOurs(Protoss.Observer))
    watchMemory = memory
    val monitor = if (grounded) None else remaining.headOption
    watchMemory = watchMemory.copy(lastIds = monitor.map(_.id).toSet)
    if ( ! grounded && MonitorWatch.applies()) {
      monitor.foreach(eye => MonitorWatch.act(this, eye))
    }
  }
}
