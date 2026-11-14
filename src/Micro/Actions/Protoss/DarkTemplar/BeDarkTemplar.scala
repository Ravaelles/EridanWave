package Micro.Actions.Protoss.DarkTemplar

import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Agency.Commander
import Micro.Targeting.DarkTemplarTargets
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.Time.Minutes
import Utilities.?

/**
 * Dark Templar brain: detectors outrank everything (evade first), then
 * snipe lone mines, otherwise hunt visible hittable enemies first and
 * freshly remembered contacts second - never unseen ground. March and
 * formations are bypassed elsewhere (no slots, no waiting, no rally), so
 * the hunt target here is the whole march program; with no contact the
 * Templar holds its decision and travels with the squad instead of
 * scouting alone.
 */
object BeDarkTemplar extends Action {

  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.DarkTemplar(unit)

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    // Detectors outrank everything: a seen Templar is a dead Templar, so
    // evade first and never hunt, snipe or hold through detection.
    if (evadeDetector(unit)) return

    // Shot is seen: while a ranged threat's reach plus the hide band
    // touches the Templar, no re-attack - run first, hunt later.
    if (revealedHide(unit)) return

    snipeSpiders(unit)
    if (unit.agent.toAttack.isEmpty) hunt(unit)
  }

  private def evadeDetector(unit: FriendlyUnitInfo): Boolean = {
    // Lone detector, full shields, nothing that can punish: hold the hunt
    // instead of running - the reveal costs nothing yet.
    if (DarkTemplarEvade.shouldHoldVsDetector(
      unit.unitClass.maxShields - unit.shieldPoints,
      unit.matchups.threats.exists(t => t.canAttack(unit) && unit.tileDistanceEdge(t) < DarkTemplarEvade.holdVsDetectorTiles))) return false

    // Attackers outrank detectors: a visible enemy that can actually
    // hit us inside thirteen tiles is the run trigger, not the Overlord
    // watching it happen - flee the guns, then the eyes.
    if (Maff.minBy(unit.matchups.threats.filter(t =>
      t.visible
      && t.canAttack(unit)
      && DarkTemplarEvade.shouldFleeAttacker(unit.pixelDistanceEdge(t) * Maff.inv32)))(
      unit.pixelDistanceEdge(_)).exists(attacker => fleeFrom(unit, attacker.pixel))) return true

    // Latched, nearest detector only: the latch is per-unit state, so one
    // call per frame keeps it consistent - a second call for a farther
    // detector would overwrite the hold. Fleeing the nearest is the
    // right move in the common single-Overlord case anyway.
    Maff.minBy(unit.matchups.groupVs.detectors.filter(_.likelyStillThere))(
      unit.pixelDistanceEdge(_)).exists(detector =>
      DarkTemplarEvade.evadeLatched(unit.id, unit.pixelDistanceEdge(detector), detector.sightPixels)
        && fleeFrom(unit, detector.pixel))
  }

  private def fleeFrom(unit: FriendlyUnitInfo, threatPixel: Mathematics.Points.Pixel): Boolean = {
    // No re-attack while evading: a stale target would drag the hunt
    // (and Combat) straight back under the detector next frame.
    unit.agent.toAttack = None
    unit.agent.act("DarkTemplarEvade")
    val away = unit.pixel.project(threatPixel, -4.0 * 32.0)
    unit.agent.destinationNext.set(?(away.walkable, away, away.walkablePixel))
    Commander.move(unit)
    true
  }

  private def revealedHide(unit: FriendlyUnitInfo): Boolean = {
    // Spider mines are sniped, not hidden from - the snipe doctrine owns them.
    Maff.minBy(unit.matchups.threats.filter(t =>
      t.canAttack(unit)
      && ! Terran.SpiderMine(t)
      && DarkTemplarEvade.shouldHide(unit.pixelDistanceEdge(t), t.pixelRangeAgainst(unit))))(
      unit.pixelDistanceEdge(_)).exists(threat => {
      // No re-attack while hiding: the guns that just found the Templar
      // outrange patience, so the slot stays empty until the band clears.
      unit.agent.toAttack = None
      unit.agent.act("DarkTemplarHide")
      val away = unit.pixel.project(threat.pixel, -4.0 * 32.0)
      unit.agent.destinationNext.set(?(away.walkable, away, away.walkablePixel))
      Commander.move(unit)
      true
    })
  }

  private def snipeSpiders(unit: FriendlyUnitInfo): Unit = {
    if (unit.readyForAttackOrder && With.enemies.exists(_.isTerran)) {
      val mines = unit.matchups.threats.filter(Terran.SpiderMine)
      if (mines.nonEmpty) {
        val mine = mines.minBy(_.pixelDistanceEdge(unit))
        val sighting = SpiderMineSniping.MineSighting(
          targetedUs        = mine.orderTarget.contains(unit),
          distPixels        = mine.pixelDistanceEdge(unit),
          neighborsInRadius = mines.count(_.pixelDistanceEdge(mine) < SpiderMineSniping.radiusPixels),
          visible           = mine.visible,
          cloaked           = mine.effectivelyCloaked)
        if (SpiderMineSniping.shouldSnipe(sighting)) {
          unit.agent.toAttack = Some(mine)
          Commander.attack(unit)
        }
      }
    }
  }

  private def hunt(unit: FriendlyUnitInfo): Unit = {
    val target = visibleHittable(unit).orElse(rememberedContact(unit))
    unit.agent.toAttack = target
    target match {
      case Some(t) =>
        unit.agent.act("DarkTemplarHunt")
        if (unit.inRangeToAttack(t)) Commander.attack(unit)
        else {
          unit.agent.destinationNext.set(t.pixel)
          Commander.move(unit)
        }
      case None =>
        unit.agent.act("DarkTemplarHold")
    }
  }

  private def visibleHittable(unit: FriendlyUnitInfo): Option[UnitInfo] = {
    // No cooldown, real contact close by: kill weakest first (a wounded
    // Marauder dies to one swipe), then tanks, warriors, workers - nearest
    // inside each tier. Otherwise the old nearest-hittable hunt holds.
    // Covered contacts are skipped: a target standing under a likely
    // detector re-arms the evade on arrival, so the hunt holds instead
    // of feeding the approach/evade loop the latch just broke.
    val open = unit.matchups.targets.filter(t =>
      t.visible && unit.canAttack(t) && ! coveredByDetector(unit, t))
    val close = open.filter(t =>
      unit.tileDistanceEdge(t) <= DarkTemplarTargets.closeTiles).toVector
    if (unit.cooldownLeft <= 0 && close.nonEmpty) {
      val marks = close.map(t => DarkTemplarTargets.Mark(
        unit.tileDistanceEdge(t),
        t.totalHealth,
        Terran.SiegeTankSieged(t) || Terran.SiegeTankUnsieged(t),
        t.unitClass.isWarrior,
        t.unitClass.isWorker))
      DarkTemplarTargets.pick(marks).map(close)
    } else {
      Maff.minBy(open.filter(t => t.visible && unit.canAttack(t)))(_.pixelDistanceEdge(unit))
    }
  }

  private def coveredByDetector(unit: FriendlyUnitInfo, target: UnitInfo): Boolean =
    DarkTemplarEvade.underDetection(
      target.pixel,
      unit.matchups.groupVs.detectors.view.filter(_.likelyStillThere).map(d => (d.pixel, d.sightPixels.toDouble)).toSeq)

  private def rememberedContact(unit: FriendlyUnitInfo): Option[UnitInfo] =
    Maff.maxBy(With.units.enemy.filter(e =>
      ! e.visible
      && unit.canAttack(e)
      && ! coveredByDetector(unit, e)
      && With.framesSince(e.lastSeen) < Minutes(3)()))(_.lastSeen)
}
