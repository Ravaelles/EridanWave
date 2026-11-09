package Tactic.Tactics.Scouting

import Tactic.Tactics.Tactic

import Lifecycle.With
import Macro.Facts.MacroFacts
import Mathematics.Maff
import Mathematics.Points.Pixel
import Planning.ResourceLocks.LockUnits
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.Time.Seconds
import Utilities.UnitCounters.CountUpTo
import Utilities.UnitFilters.{IsAny, IsMobileDetector}
import Utilities.UnitPreferences.PreferClose

class TacticMonitor extends Tactic {

  // One eye pool, assigned in priority order each frame: army escort, expo
  // reveal, natural monitoring. A single lock keeps the priority in exactly
  // one method (launch) instead of scattered across duties.
  val eyes: LockUnits = new LockUnits(this, IsAny(Protoss.Observer, Zerg.Overlord), CountUpTo(3), interruptable = false)

  private var lastMonitorFrame = -1
  private var lastMonitorIds = Set.empty[Int]
  private var monitorDeathFrame = -1

  override def launch(): Unit = {
    if (With.frame < lastMonitorFrame) {
      // New game: frames restarted, remembered ids are meaningless.
      lastMonitorIds = Set.empty
      monitorDeathFrame = -1
    }
    lastMonitorFrame = With.frame

    var bases = With.geography.enemyBases
    eyes
      .setPreference(PreferClose(With.geography.home.center))
      .acquire()
    val free = eyes.units.toVector.sortBy(_.id)
    if (bases.isEmpty) {
      // UMS: no bases to monitor or reveal; escort keeps the eye with
      // the muscle (Stardust ObserverActivity.EscortingArmy).
      free.headOption.foreach(driveEscort)
      updateMonitorMemory(Set.empty[Int])

      return
    }

    // 1. Army escort has first claim: an eye overhead once burrowed
    // threats are known while the attack squad is out (the wipe case).
    val escort = if (armyNeedsEscort) nearestEye(free, With.tactics.attackSquad.centroidKey) else None
    escort.foreach(driveEscort)

    // 2. Expo reveal: a spare eye, or the first eye when safe.
    var rest = free.filterNot(escort.contains)
    RevealExpansionBlockedWithHiddenUnit.siteToReveal().foreach(site => {
      val pool = rest
      val loneFirstEye = pool.size == 1 && escort.isEmpty
      if ( ! pool.isEmpty && ! (loneFirstEye && ! RevealExpansionBlockedWithHiddenUnit.mayUseFirstEye)) {
        Maff.minBy(pool)(_.pixelDistanceSquared(site.heart.center)).foreach(eye => {
          eye.intend(this).setTerminus(site.heart.center)
          rest = rest.filterNot(_ == eye)
        })
      }
    })

    // 3. Natural monitoring takes the leftovers, unless grounded: an eye
    // died on this duty and we are below three Observers.
    val grounded = monitorDeathFrame >= 0 && With.units.countOurs(Protoss.Observer) < 3
    val monitor = if (grounded) None else rest.headOption
    updateMonitorMemory(monitor.map(_.id).toSet)
    if ( ! With.blackboard.monitorBases())                  return
    if (With.units.existsEnemy(IsMobileDetector))           return
    if (With.units.countEnemy(Terran.Factory) > 7)          return
    if (With.enemies.exists(_.hasTech(Terran.WraithCloak))) return
    if (With.units.countOurs(Protoss.Observer) < 3 && MacroFacts.enemyHasShown(Terran.SpiderMine)) return

    val cloaked = monitor.exists(_.cloaked)
    val naturals = bases.flatMap(_.natural.filter(b => With.framesSince(b.lastFrameScoutedByUs) > Seconds(25)()))
    bases = Maff.orElse(naturals, bases).toVector
    monitor.foreach(eye => eye.intend(this)
      .setTerminus(With.geography.home.center)
      .setScout(bases.flatMap(_.zone.tiles.filter(t => t.enemyRangeAir <= 0 && ( ! cloaked || ! t.enemyDetectedUnchecked)))))
  }

  // The wipe case: burrowed threats known while the attack squad is out.
  // The escort Observer rides overhead (Spot micro covers the Lurkers)
  // instead of monitoring naturals across the map.
  private def armyNeedsEscort: Boolean =
    With.tactics.attackSquad.units.nonEmpty &&
      MacroFacts.enemyHasShown(Zerg.Lurker, Zerg.LurkerEgg)

  private def nearestEye(eyes: Seq[FriendlyUnitInfo], to: Pixel): Option[FriendlyUnitInfo] =
    Maff.minBy(eyes)(_.pixelDistanceSquared(to))

  private def driveEscort(eye: FriendlyUnitInfo): Unit = {
    val army = With.tactics.attackSquad
    val anchor = if (army.units.nonEmpty) army.centroidKey else With.geography.home.center
    Debugging.CherryVis.CherryVis.decision(eye.id, "Escorting attack squad")
    eye.intend(this)
      .setTerminus(anchor)
      .setRedoubt(With.geography.home.center)
  }

  private def updateMonitorMemory(ids: Set[Int]): Unit = {
    val died = lastMonitorIds.filterNot(id => With.units.ours.exists(_.id == id))
    if (died.nonEmpty) monitorDeathFrame = With.frame
    lastMonitorIds = ids
  }
}
