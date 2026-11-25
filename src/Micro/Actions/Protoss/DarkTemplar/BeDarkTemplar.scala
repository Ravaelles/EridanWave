package Micro.Actions.Protoss.DarkTemplar

import Micro.Actions.UnitControl
import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Agency.Commander
import Micro.Targeting.DarkTemplarTargets
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.Time.Minutes
import Utilities.Time.Seconds
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
  override def appliesToUnit(unit: FriendlyUnitInfo): Boolean = Protoss.DarkTemplar(unit)

  override def allowed(unit: FriendlyUnitInfo): Boolean = Protoss.DarkTemplar(unit)

  override protected def perform(unit: FriendlyUnitInfo): UnitControl = {
    // Unseen is unanswered: with no burn on the tile and no shots landed
    // the cloak is absolute, so the hunt pushes straight at the gun and
    // swipes anything in reach - the standoff below only owns live eyes.
    if (ghostPush(unit)) return UnitControl.claimedIf(! unit.ready)

    // Static guns outrank detectors: a remembered Cannon owns the ground
    // before its eyes are seen, so the standoff holds the approach first
    // and the evade below only owns live eyes.
    if (staticStandoff(unit)) return UnitControl.claimedIf(! unit.ready)

    // Detectors outrank everything: a seen Templar is a dead Templar, so
    // evade first and never hunt, snipe or hold through detection.
    if (evadeDetector(unit)) return UnitControl.claimedIf(! unit.ready)

    // Shot is seen: while a ranged threat's reach plus the hide band
    // touches the Templar, no re-attack - run first, hunt later.
    if (revealedHide(unit)) return UnitControl.claimedIf(! unit.ready)

    // Revealed is revealed: the tile sits under enemy detection, or
    // shots have landed - the hide is blown whether the eyes are seen
    // or not (an Observer outranges the Templar, so the detector arm
    // above never fires on an unseen eye). Flee the fix if there is
    // one, else fall back to safety - standing revealed is how
    // Templars die. The hide above owns close guns; this owns the rest.
    if (revealedWithdraw(unit)) return UnitControl.claimedIf(! unit.ready)

    snipeSpiders(unit)

    // Minefield sidestep: known mines (seen or remembered) bend the
    // path sideways around their centroid instead of through it. Radial
    // flee walks into the mine behind; a lateral step slips the field.
    // Sniping owns the frame when it answered a lone mine above.
    if (mineSidestep(unit)) return UnitControl.claimedIf(! unit.ready)

    if (unit.agent.toAttack.isEmpty) hunt(unit)
    UnitControl.claimedIf(! unit.ready)
  }

  /**
   * Mobile-ghost push (thin game seam over the pure kernel): while the tile
   * stays dark and no shot has landed, stale fog is re-verified instead
   * of cowered from - a glimpse older than twenty seconds walks, newer
   * contacts belong to the hunt and the evade. Buildings never qualify:
   * a remembered gun is walked into, not around, and masonry never moves
   * to meet us - the raid avoid and the hunt's own tiers own all masonry.
   */
  private def ghostPush(unit: FriendlyUnitInfo): Boolean = {
    if (unit.tile.enemyDetected || unit.underAttack) return false

    Maff.minBy(With.units.enemy.view.filter(e =>
      e.likelyStillThere && e.unitClass.isWarrior && ! e.unitClass.isBuilding && With.framesSince(e.lastSeen) > Seconds(20)()))(
      unit.pixelDistanceEdge(_)).exists(gun => {
      Maff.minBy(unit.matchups.targets.filter(t =>
        t.visible && unit.canAttack(t) && DarkTemplarStaticPush.inPushRange(
          unit.pixelDistanceEdge(t), unit.pixelRangeAgainst(t))))(_.totalHealth) match {
        case Some(prey) =>
          unit.agent.toAttack = Some(prey)
          unit.agent.act("DarkTemplarGhostPush")
          Commander.attack(unit)

          true
        case None =>
          unit.agent.toAttack = None
          unit.agent.act("DarkTemplarGhostPush")
          unit.agent.forceTo(gun.pixel)
          Commander.move(unit)

          true
      }
    })
  }

  /**
   * Static-gun standoff (thin game seam over the pure kernel): the nearest
   * remembered gun sets the band, velocity says closing (the gun sees us
   * coming through `isOtherUnitFacingUs`), facing says leaving (our back
   * is turned through `showingBackTo`) - a stop would re-face the guns,
   * so only the stride owns that frame. A hittable prey in reach still
   * dies; the hold only owns empty ground.
   */
  private def staticStandoff(unit: FriendlyUnitInfo): Boolean = {
    Maff.minBy(With.units.enemy.view.filter(e =>
      e.likelyStillThere && (e.unitClass.isCombatBuilding || Terran.Bunker(e))))(
      unit.pixelDistanceEdge(_)).exists(gun => {
      val danger = DarkTemplarStaticStandoff.dangerTiles(
        gun.unitClass.pixelRangeMax * Maff.inv32,
        gun.sightPixels * Maff.inv32)
      val dist = unit.pixelDistanceEdge(gun) * Maff.inv32
      val approaching = gun.isOtherUnitFacingUs(unit)
      val back = unit.showingBackTo(gun)
      DarkTemplarStaticStandoff.standoffLatched(unit.id, dist, danger, approaching, back) && (
        if (DarkTemplarStaticStandoff.shouldStop(dist, danger, approaching, back)) holdStatic(unit)
        else backOffStatic(unit, gun))
    })
  }

  private def holdStatic(unit: FriendlyUnitInfo): Boolean = {
    // A hittable prey in reach still dies: the hold owns the ground,
    // not the cooldown - standing inside the band without shooting
    // donates the very volley the stop was bought to keep.
    Maff.minBy(unit.matchups.targetsInRange.filter(t => DarkTemplarTargets.isPrey(t.unitClass)))(_.totalHealth) match {
      case Some(prey) =>
        unit.agent.toAttack = Some(prey)
        unit.agent.act("DarkTemplarStaticStop")
        Commander.attack(unit)

        true
      case None =>
        unit.agent.toAttack = None
        unit.agent.act("DarkTemplarStaticStop")
        Commander.hold(unit)

        true
    }
  }

  private def backOffStatic(unit: FriendlyUnitInfo, gun: UnitInfo): Boolean = {
    unit.agent.toAttack = None
    unit.agent.act("DarkTemplarStaticStep")
    val away = DarkTemplarStaticStandoff.stepAway(unit.pixel, gun.pixel)
    unit.agent.forceTo(?(away.walkable, away, away.walkablePixel))
    Commander.move(unit)

    true
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

    // A mine inside the pack is pack, not trap: two or more enemies
    // within a tile of any sidestep mine vetoes the dodge, or the hunt
    // walks away from the fight it came for. Mines never count as
    // their own crowd - a lone minefield still gets stepped around.
    val crowded = mines.exists(mine =>
      With.units.enemy.count(e =>
        e.alive && ! Terran.SpiderMine(e) && ! e.unitClass.isBuilding &&
          e.pixelDistanceEdge(mine) <= SpiderMineSniping.crowdRadiusTiles * 32.0) >= SpiderMineSniping.crowdFoes)
    if (crowded) return false

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
        // Lone raiders march on the enemy base instead of holding empty
        // ground: standing still donates time while the hunt has nothing,
        // and the squad's rejoin leg does not apply to hunters operating
        // alone. Only when the base is known; an unscouted map keeps the
        // old hold-and-rejoin.
        With.scouting.enemyMain match {
          case Some(base) =>
            unit.agent.act("DarkTemplarMarch")
            unit.agent.forceTo(base.heart.center)
            Commander.move(unit)
          case None =>
            unit.agent.act("DarkTemplarHold")
            rejoinSquad(unit)
        }
    }
  }

  /**
   * Back to a fighting squad: the hunt holds nothing, the hull stands
   * contactless past the drift band while the squad fights without it.
   * The cluster cohesion leg owns this on macro maps; the hunt owns it
   * here (UMS, lone squads) - same band, same label.
   */
  private def rejoinSquad(unit: FriendlyUnitInfo): Boolean = {
    val squad = unit.squad.getOrElse(return false)
    val centroid = ?(unit.flying, squad.centroidAir, squad.centroidGround)
    val contactTiles = 2.0
    if ( ! DarkTemplarPursuit.shouldRejoinSquad(
      distTilesToSquad  = unit.pixelDistanceCenter(centroid) * Maff.inv32,
      squadFighting     = squad.units.exists(u => u != unit && u.unitClass.isWarrior && u.battle.isDefined),
      hasLocalContact   = unit.matchups.enemies.exists(e => unit.pixelDistanceEdge(e) < unit.pixelRangeMax + contactTiles * 32.0))) return false

    unit.agent.act("ClusterRejoin")
    unit.agent.forceTo(centroid)
    Commander.move(unit)
    true
  }

  private def visibleHittable(unit: FriendlyUnitInfo): Option[UnitInfo] = {
    // No cooldown, real contact close by: kill weakest first (a wounded
    // Marauder dies to one swipe), then tanks, warriors, workers - nearest
    // inside each tier. Otherwise the old nearest-hittable hunt holds.
    // Covered contacts are skipped: a target standing under a likely
    // detector re-arms the evade on arrival, so the hunt holds instead
    // of feeding the approach/evade loop the latch just broke.
    // No duel without a local edge: a lone hull walking into a pack that
    // outguns it dies unconnected, so each pick's local pack must lose
    // the power comparison first. An unseen Templar answers everything.
    val open = unit.matchups.targets.filter(t =>
      t.visible && unit.canAttack(t) && DarkTemplarTargets.isPrey(t.unitClass) && ! coveredByDetector(unit, t) && ! raidLeashed(unit, t))
    val brave = open.filter(t => packDuelWorthIt(unit, t, open))
    // Unfinished static defense first: a morphing cannon or raising
    // turret never shoots back, so the blade walks it down from anywhere
    // instead of chewing the base past it (and letting it finish into
    // detection). Coverage and raid range already gated above.
    val unfinished = brave.view.map(t => (t,
      DarkTemplarTargets.Mark(
        unit.tileDistanceEdge(t),
        t.totalHealth,
        isTank = false,
        isWarrior = false,
        isUnfinishedStatic = DarkTemplarTargets.isUnfinishedStatic(t.unitClass.isStaticDefense, t.complete)))).toSeq
    DarkTemplarTargets.pickUnfinished(unfinished.map(_._2)) match {
      case Some(i) => return Some(unfinished(i)._1)
      case None =>
    }
    val close = brave.filter(t =>
      unit.tileDistanceEdge(t) <= DarkTemplarTargets.closeTiles).toVector
    if (unit.cooldownLeft <= 0 && close.nonEmpty) {
      val marks = close.map(t => DarkTemplarTargets.Mark(
        unit.tileDistanceEdge(t),
        t.totalHealth,
        Terran.SiegeTankSieged(t) || Terran.SiegeTankUnsieged(t),
        t.unitClass.isWarrior))
      DarkTemplarTargets.pick(marks).map(close)
    } else {
      Maff.minBy(brave)(_.pixelDistanceEdge(unit))
    }
  }

  /**
   * Duel the pack around this pick (thin game seam over the bravery
   * kernel): my hull plus nearby warrior friends vs the local guns.
   * Armor reads at class level - wounds and numbers move the sums, not
   * the per-shot price. Cloaked means unanswerable, so the incoming
   * side reads zero and the duel is free.
   */
  private def packDuelWorthIt(unit: FriendlyUnitInfo, pick: UnitInfo, candidates: Seq[UnitInfo]): Boolean = {
    val pack = candidates.filter(c => pick.pixelDistanceEdge(c) * Maff.inv32 <= DarkTemplarBravery.packTiles)
    val ourDps = groundDps(unit.unitClass) + unit.squad.toSeq.flatMap(_.units)
      .filter(u => u != unit && u.alive && u.unitClass.isWarrior && pick.pixelDistanceEdge(u) * Maff.inv32 <= DarkTemplarBravery.packTiles)
      .map(u => groundDps(u.unitClass)).sum
    // Unfinished statics deal nothing: a morphing cannon prices like a
    // live gun by class, so the duel would veto the free kill. Zero it
    // and the pack math approves walking it down; live guns guarding it
    // still veto through the rest of the pack.
    val foeDps = ?(unit.effectivelyCloaked, 0.0, pack.map(f =>
      ?(f.unitClass.isStaticDefense && ! f.complete, 0.0, groundDps(f.unitClass))).sum)
    DarkTemplarBravery.shouldDuel(
      unit.totalHealth, ourDps,
      pack.map(_.totalHealth).sum, foeDps)
  }

  private def groundDps(unitClass: UnitClass): Double =
    unitClass.effectiveGroundDamage.toDouble / unitClass.groundDamageCooldown.max(1)

  /**
   * Raid leash (thin game seam over the pure hold): workers and lone
   * warriors deep over the enemy main are not approached past dueling
   * distance. Without a scouted enemy main there is nothing to
   * measure against, so the hunt runs unclipped.
   */
  private def raidLeashed(unit: FriendlyUnitInfo, target: UnitInfo): Boolean = {
    val mainPx = With.scouting.enemyMain.map(_.heart.center).getOrElse(return false)
    val othersNear = With.units.enemy.count(e =>
      e.alive && e.id != target.id && e.unitClass.isWarrior &&
        e.tileDistanceEdge(target) <= DarkTemplarBravery.packTiles * 32.0)
    DarkTemplarRaidLeash.holdRaid(
      target.unitClass.isWorker,
      target.unitClass.isWarrior && othersNear == 0,
      target.tile.groundPixels(mainPx) * Maff.inv32,
      unit.tileDistanceEdge(target))
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
      && ! raidLeashed(unit, e)
      && ! DarkTemplarPursuit.ghostDropped(
        unit.id, e.id,
        arrivedUnseen = DarkTemplarPursuit.shouldDropGhost(unit.tileDistanceEdge(e), e.visible),
        recentlySeen  = With.framesSince(e.lastSeen) <= DarkTemplarPursuit.recentSeenFrames)
      && With.framesSince(e.lastSeen) < Minutes(3)()))(_.lastSeen)
}
