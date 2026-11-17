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
  override def isContainer: Boolean = true

  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.DarkTemplar(unit)

  override protected def perform(unit: FriendlyUnitInfo): Unit = {
    // Detectors outrank everything: a seen Templar is a dead Templar, so
    // evade first and never hunt, snipe or hold through detection.
    if (evadeDetector(unit)) return

    // Shot is seen: while a ranged threat's reach plus the hide band
    // touches the Templar, no re-attack - run first, hunt later.
    if (revealedHide(unit)) return

    // Revealed is revealed: the tile sits under enemy detection, or
    // shots have landed - the hide is blown whether the eyes are seen
    // or not (an Observer outranges the Templar, so the detector arm
    // above never fires on an unseen eye). Flee the fix if there is
    // one, else fall back to safety - standing revealed is how
    // Templars die. The hide above owns close guns; this owns the rest.
    if (revealedWithdraw(unit)) return

    snipeSpiders(unit)

    // Minefield sidestep: known mines (seen or remembered) bend the
    // path sideways around their centroid instead of through it. Radial
    // flee walks into the mine behind; a lateral step slips the field.
    // Sniping owns the frame when it answered a lone mine above.
    if (mineSidestep(unit)) return

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
      unit.pixelDistanceEdge(_)).exists(attacker => fleeAway(unit, attacker.pixel, "DarkTemplarEvade"))) return true

    // Latched, nearest detector only: the latch is per-unit state, so one
    // call per frame keeps it consistent - a second call for a farther
    // detector would overwrite the hold. Fleeing the nearest is the
    // right move in the common single-Overlord case anyway.
    // Mines are not eyes: their trigger radius, not their sight, sets
    // the rule - they get their own sidestep below instead of the
    // nine-tile detector evade that pinned hulls off the hunt.
    Maff.minBy(unit.matchups.groupVs.detectors.filter(d =>
      d.likelyStillThere && ! Terran.SpiderMine(d)))(
      unit.pixelDistanceEdge(_)).exists(detector =>
      DarkTemplarEvade.evadeLatched(unit.id, unit.pixelDistanceEdge(detector), detector.sightPixels)
        && fleeAway(unit, detector.pixel, "DarkTemplarEvade"))
  }

  /**
   * Run straight off a threat, dropping the target first: a stale target
   * would drag the hunt (and Combat) straight back under the guns next
   * frame. Evade and hide are the same move with different labels.
   */
  private def fleeAway(unit: FriendlyUnitInfo, threatPixel: Mathematics.Points.Pixel, label: String): Boolean = {
    unit.agent.toAttack = None
    unit.agent.act(label)
    val away = unit.pixel.project(threatPixel, -4.0 * 32.0)
    unit.agent.forceTo(?(away.walkable, away, away.walkablePixel))
    Commander.move(unit)
    true
  }

  private def revealedHide(unit: FriendlyUnitInfo): Boolean = {
    // Spider mines are sniped, not hidden from - the snipe doctrine owns them.
    Maff.minBy(unit.matchups.threats.filter(t =>
      t.canAttack(unit)
      && ! Terran.SpiderMine(t)
      && DarkTemplarEvade.shouldHide(unit.pixelDistanceEdge(t), t.pixelRangeAgainst(unit))))(
      unit.pixelDistanceEdge(_)).exists(threat =>
      // No re-attack while hiding: the guns that just found the Templar
      // outrange patience, so the slot stays empty until the band clears.
      fleeAway(unit, threat.pixel, "DarkTemplarHide"))
  }

  private def revealedWithdraw(unit: FriendlyUnitInfo): Boolean = {
    if ( ! DarkTemplarEvade.shouldWithdrawRevealed(unit.tile.enemyDetected, unit.underAttack)) return false
    // The eyes that burn, nearest first: a remembered fix is enough to
    // run from - the evade band already held the approach loop, this
    // only picks the direction.
    Maff.minBy(unit.matchups.groupVs.detectors.filter(d =>
      d.likelyStillThere && ! Terran.SpiderMine(d)))(
      unit.pixelDistanceEdge(_)) match {
      case Some(detector) => fleeAway(unit, detector.pixel, "DarkTemplarReveal")
      case None =>
        // No fix at all: revealed by unseen eyes or shot from the fog.
        // Fall back to safety instead of holding revealed ground.
        unit.agent.toAttack = None
        unit.agent.act("DarkTemplarReveal")
        val safety = unit.agent.safety
        unit.agent.forceTo(?(safety.walkable, safety, safety.walkablePixel))
        Commander.move(unit)
        true
    }
  }

  private def mineSidestep(unit: FriendlyUnitInfo): Boolean = {
    if (unit.agent.toAttack.isDefined) return false

    val mines = With.units.enemy.view.filter(e =>
      Terran.SpiderMine(e)
        && e.likelyStillThere
        && unit.pixelDistanceEdge(e) <= SpiderMineSniping.sidestepTiles * 32.0).toSeq
    if (mines.isEmpty) return false

    val step = SpiderMineSniping.sidestepPixel(
      unit.pixel,
      Maff.centroid(mines.map(_.pixel)),
      SpiderMineSniping.sidestepStepTiles,
      goLeft = unit.id % 2 == 0)
    unit.agent.act("MineSidestep")
    unit.agent.forceTo(?(step.walkable, step, step.walkablePixel))
    Commander.move(unit)

    true
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
          unit.agent.forceTo(t.pixel)
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
      t.visible && unit.canAttack(t) && DarkTemplarTargets.isPrey(t.unitClass) && ! coveredByDetector(unit, t))
    val close = open.filter(t =>
      unit.tileDistanceEdge(t) <= DarkTemplarTargets.closeTiles).toVector
    if (unit.cooldownLeft <= 0 && close.nonEmpty) {
      val marks = close.map(t => DarkTemplarTargets.Mark(
        unit.tileDistanceEdge(t),
        t.totalHealth,
        Terran.SiegeTankSieged(t) || Terran.SiegeTankUnsieged(t),
        t.unitClass.isWarrior))
      DarkTemplarTargets.pick(marks).map(close)
    } else {
      Maff.minBy(open)(_.pixelDistanceEdge(unit))
    }
  }

  private def coveredByDetector(unit: FriendlyUnitInfo, target: UnitInfo): Boolean =
    DarkTemplarEvade.underDetection(
      target.pixel,
      unit.matchups.groupVs.detectors.view
        .filter(d => d.likelyStillThere && ! Terran.SpiderMine(d))
        .map(d => (d.pixel, d.sightPixels.toDouble)).toSeq)

  private def rememberedContact(unit: FriendlyUnitInfo): Option[UnitInfo] =
    Maff.maxBy(With.units.enemy.filter(e =>
      ! e.visible
      && unit.canAttack(e)
      && DarkTemplarTargets.isPrey(e.unitClass)
      && ! coveredByDetector(unit, e)
      && With.framesSince(e.lastSeen) < Minutes(3)()))(_.lastSeen)
}
