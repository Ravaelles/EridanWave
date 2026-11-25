package Tactic.Tactics.Scouting

import Tactic.Tactics.Tactic

import Information.Geography.Types.Base
import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import ProxyBwapi.UnitTracking.UnorderedBuffer
import Strategery.Strategies.Protoss.PvEProxy2Gate
import Strategery.Strategies.Zerg.ZvE4Pool
import Utilities.?
import Utilities.Time.Seconds
import Utilities.UnitCounters.CountUpTo
import Utilities.UnitFilters.{IsAny, IsComplete, IsSpeedling, IsTank, IsWorker}
import Utilities.UnitPreferences.PreferScout

class TacticWorkerScout extends Tactic {

  var scoutingAbandoned : Boolean                   = false
  var scouts            : Vector[FriendlyUnitInfo]  = Vector.empty

  lock.setMatcher(IsWorker).setInterruptible(false)

  def launch(): Unit = {
    if (scoutingAbandoned) return

    val basesToScout = Maff.orElseFiltered(
      ?(With.scouting.enemyMainFullyScouted,
        With.geography.enemyBases.flatMap(b => Vector(b) ++ b.natural ++ b.naturalOf).distinct,
        With.scouting.enemyMain.toVector),
      With.geography.mains.filterNot(_.scoutedByUs),
      With.geography.neutralBases)( ! _.island).toVector

    scoutingAbandoned ||= scouts.exists( ! _.alive)
    scoutingAbandoned ||= basesToScout.isEmpty

    if ( ! ZvE4Pool() && ! PvEProxy2Gate()) {
      // Abandon scouting when we have a closer unit approaching
      scoutingAbandoned ||= (
        With.blackboard.wantToAttack()
        && scouts.nonEmpty
        && vicinity.metro.exists(m => ! scouts.exists(_.metro.contains(m)))
        && With.units.ours.exists(u =>
          u.complete
          && ! IsWorker(u)
          && With.geography.enemyBases.view.map(_.heart).exists(h => scouts.forall(_.framesToTravelTo(h) > u.framesToTravelTo(h) - Seconds(5)()))))

      // Abandon scouting when they can catch our scout (and we're not counting on the scout to help fight)
      scoutingAbandoned ||= With.units.enemy.exists(_.isAll(
        IsComplete,
        IsAny(
          Terran.Marine,
          Terran.Vulture,
          Terran.Factory,
          IsTank,
          Protoss.Dragoon,
          IsSpeedling,
          Zerg.Hydralisk,
          Zerg.Mutalisk,
          Zerg.Spire)))
      scoutingAbandoned ||= With.units.enemy.exists(e => e.complete && e.isAny(Protoss.PhotonCannon, Zerg.SunkenColony) && scouts.forall(_.proximity > e.proximity))
    }

    if (scoutingAbandoned) {
      return
    }

    val scoutCount = Maff.vmin(
      With.blackboard.maximumScouts(),
      With.units.countOurs(IsWorker) - 3,
      basesToScout.map(_.metro).distinct.size)
    if (lock.units.size > scoutCount) { lock.release() }

    scouts = lock
      .setCounter(CountUpTo(scoutCount))
      .setPreference(new PreferScout(basesToScout.map(_.townHallArea.center): _*))
      .acquire().toVector // The copy is important since the source is mutable

    allocateScouts(basesToScout, scouts)
  }

  def allocateScouts(basesToScout: Seq[Base], scouts: Seq[FriendlyUnitInfo]): Unit = {

    if (scouts.isEmpty) return

    val enemyHasCombatUnits = With.units.enemy.exists(u => u.canAttack && ! IsWorker(u))
    val orderedScouts       = scouts.sortBy(s => basesToScout.view.map(_.townHallTile).map(s.pixelDistanceTravelling).min)
    val basesToScoutQueue   = new UnorderedBuffer[Base]()
    // Minute check of the enemy third: the closest base past the natural,
    // stale past sixty seconds. The head scout walks it directly instead
    // of joining the nearest-first queue, so the location can never sit
    // unchecked for minutes again.
    val forcedThird         = staleEnemyThird.map(orderedScouts.head -> _).toMap

    orderedScouts.indices.foreach(i => {
      if (basesToScoutQueue.isEmpty) {
        basesToScoutQueue.addAll(basesToScout)

        // For bookkeeping/debugging; vicinity is not actually used
        basesToScoutQueue.headOption.foreach(b => vicinity = b.heart.center)
      }
      val scout = orderedScouts(i)
      forcedThird.get(scout) match {
        case Some(third) => sendScoutTo(scout, third, basesToScoutQueue, enemyHasCombatUnits)
        case None =>
          val ovieBase  = With.tactics.scoutWithOverlord.vicinity.base.filter(b => With.tactics.scoutWithOverlord.units.nonEmpty)
          val base      = Maff
            .orElse(basesToScoutQueue.filter(_.isCross || i > 0 || ! With.blackboard.crossScout()), basesToScoutQueue)
            .minBy(b => scout.pixelDistanceTravelling(b.townHallTile) + 32 * 64 * Maff.fromBoolean(ovieBase.contains(b)))
          sendScoutTo(scout, base, basesToScoutQueue, enemyHasCombatUnits)
      }
    })
  }

  private def sendScoutTo(
    scout               : FriendlyUnitInfo,
    base                : Base,
    basesToScoutQueue   : UnorderedBuffer[Base],
    enemyHasCombatUnits : Boolean): Unit = {
    val bases     = Seq(base) ++ base.natural.filter(natural => ! enemyHasCombatUnits && ! base.tiles.exists(t => t.buildable && ! t.explored))
    val explored  = base.townHallArea.tiles.exists(_.explored)
    bases.foreach(basesToScoutQueue.remove)
    scout.intend(this)
      .setTerminus(base.townHallArea.cornerPixels.minBy(scout.pixelDistanceTravelling))
      .setScout(
        if (explored) bases.flatMap(_.tiles.view.filter(t => t.buildable && With.grids.enemyRangeGround(t) <= With.grids.enemyRangeGround.margin))
        else          Seq(base.townHallArea.tiles.minBy(scout.pixelDistanceTravelling)))
  }

  /**
   * Enemy third due for its minute check (no game decisions here, just
   * the pick): the nearest base past the natural, unseen for sixty
   * seconds. The base layout never changes, so the pick caches after the
   * enemy main is known; only the staleness re-arms. The cache is keyed on
   * that main, since that is the input the pick depends on (a re-identified
   * main must not leave the old third walking forever). All races - a
   * hidden third kills against anyone.
   */
  private var cachedThird      : Option[Base] = None
  private var cachedThirdMain  : Option[Pixel] = None

  private def staleEnemyThird: Option[Base] = {
    if ( ! With.scouting.enemyMainFullyScouted) return None

    val mainPixel = With.scouting.enemyMain.map(_.heart.center)
    if (cachedThird.isEmpty || cachedThirdMain != mainPixel) {
      cachedThirdMain = mainPixel
      cachedThird     = With.scouting.enemyMain.flatMap(main =>
        With.geography.bases.filter(b => ! b.island && b != main).toVector
          .sortBy(b => main.heart.groundPixels(b.heart))
          .find(b => ! main.natural.contains(b)))
    }
    cachedThird.filter(third => With.framesSince(third.lastFrameScoutedByUs) > Seconds(60)())
  }
}
