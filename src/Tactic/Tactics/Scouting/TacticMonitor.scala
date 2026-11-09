package Tactic.Tactics.Scouting

import Tactic.Tactics.Tactic

import Lifecycle.With
import Macro.Facts.MacroFacts
import Mathematics.Maff
import Planning.ResourceLocks.LockUnits
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import Utilities.Time.Seconds
import Utilities.UnitCounters.CountOne
import Utilities.UnitFilters.{IsAny, IsMobileDetector}
import Utilities.UnitPreferences.PreferClose

class TacticMonitor extends Tactic {

  val scouts: LockUnits = new LockUnits(this, IsAny(Protoss.Observer, Zerg.Overlord), CountOne, interruptable = false)
  // Army escort outranks monitoring: one eye overhead when burrowed threats
  // are known (Atlantis FollowArmy). Expo reveal goes second (Atlantis
  // DetectNewBasePotentiallyBlocked); monitoring takes the leftovers.
  val escorts: LockUnits = new LockUnits(this, IsAny(Protoss.Observer, Zerg.Overlord), CountOne, interruptable = false)
  val revealers: LockUnits = new LockUnits(this, IsAny(Protoss.Observer, Zerg.Overlord), CountOne, interruptable = false)

  override def launch(): Unit = {
    var bases = With.geography.enemyBases
    if (bases.isEmpty || armyNeedsEscort) {
      escortArmy()

      if (bases.isEmpty) return
    }

    revealExpoIfNeeded()

    if ( ! With.blackboard.monitorBases())                  return
    if (With.units.existsEnemy(IsMobileDetector))           return
    if (With.units.countEnemy(Terran.Factory) > 7)          return
    if (With.enemies.exists(_.hasTech(Terran.WraithCloak))) return
    if (With.units.countOurs(Protoss.Observer) < 3 && MacroFacts.enemyHasShown(Terran.SpiderMine)) return

    scouts
      .setPreference(PreferClose(scouts.units.headOption.map(_.pixel).getOrElse(With.geography.home.center)))
      .acquire()
    val cloaked = scouts.units.exists(_.cloaked)
    val naturals = bases.flatMap(_.natural.filter(b => With.framesSince(b.lastFrameScoutedByUs) > Seconds(25)()))
    bases = Maff.orElse(naturals, bases).toVector
    scouts.units.foreach(_.intend(this)
      .setTerminus(With.geography.home.center)
      .setScout(bases.flatMap(_.zone.tiles.filter(t => t.enemyRangeAir <= 0 && ( ! cloaked || ! t.enemyDetectedUnchecked)))))
  }

  // The wipe case: burrowed threats known while the attack squad is out.
  // The escort Observer rides overhead (Spot micro covers the Lurkers)
  // instead of monitoring naturals across the map.
  private def armyNeedsEscort: Boolean =
    With.tactics.attackSquad.units.nonEmpty &&
      MacroFacts.enemyHasShown(Zerg.Lurker, Zerg.LurkerEgg)

  // A planned base with undiscovered enemy presence gets the nearest free
  // eye before we build there. Needs a spare: the army's detector stays.
  private def revealExpoIfNeeded(): Unit = {
    if (With.units.countOurs(Protoss.Observer) < 2) return

    val site = With.geography.bases.find(b =>
      b.plannedExpoRecently && With.units.enemy.exists(e =>
        ! e.flying && e.pixelDistanceCenter(b.heart.center) < 4 * 32))
    site.foreach(base => {
      revealers
        .setPreference(PreferClose(base.heart.center))
        .acquire()
      revealers.units.foreach(scout => {
        scout.intend(this).setTerminus(base.heart.center)
      })
    })
  }

  // Baseless (UMS) maps have no bases to monitor. The observer still needs an
  // owner, so it escorts the attack squad - staying with the muscle where it
  // can spot cloaked threats (Stardust ObserverActivity.EscortingArmy).
  // Falls back to home while the attack squad is still empty.
  private def escortArmy(): Unit = {
    escorts.acquire()
    val army = With.tactics.attackSquad
    val anchor = if (army.units.nonEmpty) army.centroidKey else With.geography.home.center
    escorts.units.foreach(scout => {
      Debugging.CherryVis.CherryVis.decision(scout.id, "Escorting attack squad")
      scout.intend(this)
        .setTerminus(anchor)
        .setRedoubt(With.geography.home.center)
    })
  }
}


