package Tactic.Squads.support

import Lifecycle.With
import Mathematics.Maff
import Performance.Cache
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import Utilities.?
import Utilities.Time.{GameTime, Minutes, Seconds}
import Utilities.UnitCounters.CountOne
import Utilities.UnitFilters._
import Utilities.UnitPreferences.{PreferAll, PreferClose, PreferIf}
import Tactic.Squads.Squad
import Tactic.Squads.SquadTargeting
import Tactic.Squads.Squads

class SquadScoutExpansions extends Squad {

  def frameToScout: GameTime = {
    if (With.self.isProtoss) {
      if      (With.enemy.isTerran)   GameTime(9, 0)
      else if (With.enemy.isProtoss)  GameTime(9, 0)
      else                            GameTime(7, 0)
    } else if (With.self.isTerran) {
      if      (With.enemy.isTerran)   GameTime(8, 0)
      else if (With.enemy.isProtoss)  GameTime(7, 0)
      else                            GameTime(7, 0)
    } else {
      if      (With.enemy.isTerran)   GameTime(9, 0)
      else if (With.enemy.isProtoss)  GameTime(9, 0)
      else                            GameTime(12, 0)
    }
  }

  private val scoutClasses = Seq(
    Terran.Marine,
    Terran.Firebat,
    Terran.Vulture,
    Terran.Goliath,
    Terran.Wraith,
    Protoss.Zealot,
    Protoss.Dragoon,
    Protoss.Observer,
    Protoss.Corsair,
    Zerg.Zergling,
    Zerg.Hydralisk,
    Zerg.Overlord,
    Zerg.Scourge,
    Zerg.Queen)
  val matchAll    : UnitFilter = IsAny((scoutClasses :+ Is(scoutZergThird && IsWorker(_))): _*)
  val matchFlying : UnitFilter = IsAny(scoutClasses.filter(_.canFly): _*)

  val scoutableBases = new Cache(() =>
    With.geography.neutralBases
      .view
      .filterNot(With.scouting.enemyMain.contains)
      .filterNot(With.scouting.enemyNatural.contains)
      .filterNot(b =>
        With.scouting.enemyMain.exists(m =>
          m.metro == b.metro
          && With.scouting.enemyNatural.exists(n =>
            b.heart.groundTiles(m.heart) < n.heart.groundTiles(m.heart))))
      .toVector
      .sortBy(base => - With.scouting.baseIntrigue.getOrElse(base, 0.0))
      .sortBy(b => lock.units.exists(_.flying) || b.island))

  val scoutZergCutoff: Int = Minutes(4)()
  def scoutZergThird: Boolean = (
    With.frame < scoutZergCutoff
    && ! With.self.isZerg
    && With.enemies.exists(_.isZerg)
    && With.tactics.workerScout.scoutingAbandoned)

  def launch(): Unit = {
    if ( ! With.blackboard.scoutExpansions()) return

    // User rule: from eighty supply a Dragoon keeps the enemy third and
    // fourth scouted, even while the army regroups - blind guesses mill
    // the game away the same way blind pushes do.
    val dragoonScout = With.self.isProtoss && ExpansionScout.wantsDragoonScout(With.self.supplyUsed200)
    if ( ! scoutZergThird) {
      if (With.frame < frameToScout() && ! dragoonScout) return
      if ( ! With.blackboard.wantToAttack() && ! With.blackboard.wantToHarass() && ! dragoonScout) return
    }

    if (scoutableBases().isEmpty) return

    val toScoutBases = Maff.orElse(
      scoutableBases().filter(b => With.framesSince(b.lastFrameScoutedByUs) > ExpansionScout.revisitFrames),
      scoutableBases().filter(b => With.framesSince(b.lastFrameScoutedByUs) > Seconds(30)()),
      scoutableBases())
    val toScoutBaseSorted = toScoutBases.toVector.sortBy(b =>
      Maff.maxBy(units)(_.squadAge).map(_.framesToTravelTo(b.heart))
      .getOrElse(b.heart.groundTiles(With.geography.home)))

    vicinity        = toScoutBaseSorted.head.heart.center
    lock
      .setCounter(CountOne)
      .setMatcher(?(dragoonScout || (With.blackboard.wantToAttack() && With.scouting.enemyProximity < 0.65), matchAll, matchFlying))
      .setPreference(?(dragoonScout,
        PreferAll(PreferIf(Protoss.Dragoon(_)), PreferClose(vicinity)),
        PreferClose(vicinity)))
      .acquire()
  }

  def run(): Unit = {
    units.foreach(u => {
      val targetsHere   = u.base.toSeq.filter(_.isEnemy).flatMap(_.enemies)
      val targetsThere  = vicinity.base.toSeq.flatMap(_.enemies)
      // Smooth travel like worker scouts: one far order, no waypoint hops.
      u.intend(this)
        .setCanSneak(true)
        .setShouldCorrectXY(false)
        .setTerminus(vicinity)
        .setTargets(SquadTargeting.rankForArmy(
          this,
          (targetsHere ++ targetsThere)
            .filter(_.caughtBy(u))
            .toSet
            .toVector))
    })
  }
}

