package Micro.Actions.Combat.Decisioning

import Lifecycle.With
import Mathematics.{Maff, WoundMargin}
import Micro.Actions.Combat.Decisioning.Kiting.{ClusterSafety, FacingAway, HoldToShoot, KiteAway, KiteHold, KiteYield, MeleeKiteAway}
import Micro.Agency.{Commander, OrderContinue}
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.UnitFilters.{IsSlowling, IsSpeedling}

/**
 * Kite behavior (was CombatEngagement.kiteAway): wounded units yield ground
 * instead of dying in place. Runs before the dress line (withdrawing beats
 * dressing), only while an enemy presses within range plus margin and the
 * weapon still needs its cooldown. A committed chaser met with a ready gun
 * holds to shoot instead (HoldToShoot); turned-away melee never triggers
 * the retreat (FacingAway). Returns true when it moved or held the unit.
 */
object KiteAwayBehavior {

  /**
   * Kill-switch for the whole kite behavior (kept for experiments;
   * normal play leaves it on).
   */
  var kiteEnabled: Boolean = true

  /** Held kite headings by unit: re-deriving every frame turns forever. */
  private val steadyHeadings = new scala.collection.mutable.HashMap[Int, (Double, Int)]()

  def run(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! kiteEnabled) return false
    if (With.yolo.active) return false
    // A banked stock buys the trade: expendable units hold their DPS
    // instead of yielding ground while cooling down.
    if (KiteAway.skipKiting(With.self.minerals)) return false
    if ( ! unit.canMove) return false
    if (staticStandoff(unit, combat)) return true
    if ( ! kitable(unit)) return false

    val nearest = unit.matchups.targetNearest
    if (nearest.isEmpty) return false
    // Absolute contact rule (owner order): a cooling dragoon with blades
    // on top always yields - bypasses every gate below by construction.
    // Loaded guns never reach it (their cooldown sits below the bar).
    if (KiteAway.mustFleeContact(
      Protoss.Dragoon(unit),
      unit.cooldownLeft,
      unit.matchups.threatsInPixels(KiteAway.contactKiteTiles * 32.0).exists(t =>
        t.likelyStillThere && t.unitClass.melee))) {
      kiteAway(unit, combat, hasMeleeNearby = true)

      return true
    }
    // Matchup scoping (owner order): the no-kite refinements below are
    // PvP-only. Against Zerg the classic ladder decides, held only by
    // fresh shields. Terran never kites, period - the whole leg stands
    // down and the duel owns those frames instead (unvalidated so far;
    // ladder judges). Other matchups run the ladder untouched.
    val enemyIsProtoss = With.enemies.exists(_.isProtoss)
    val enemyIsZerg = With.enemies.exists(_.isZerg)
    val enemyIsTerran = With.enemies.exists(_.isTerran)
    // ShieldWound gates need shields to read: shieldless hulls (a
    // drafted Vulture, mind-controlled steel) always read zero, so the
    // refinements below would pin them standing forever. They keep the
    // classic ladder instead.
    val shielded = unit.unitClass.maxShields > 0
    val zergRules = enemyIsZerg && ! enemyIsProtoss
    val pvpRules = enemyIsProtoss
    if (enemyIsTerran && ! enemyIsProtoss && ! enemyIsZerg) return false
    // Fresh, loaded and clear hulls hold their fire instead of kiting:
    // the kite donated the shooting on the ZealDrag mirror, so a hull
    // with the gun inside its window, enough hull to trade, and nothing
    // in contact never enters the kite decision at all. PvP-only. A lone
    // blade chase is the exception: with no guns nearby the duel is
    // free, so a fresh hull yields before contact instead of after the
    // first hit - the contact kite below owns those frames instead.
    // Lone blades press differently depending on ranged company: with no
    // guns inside the chase radius the duel is free, so a fresh hull
    // yields before contact instead of after the first hit - the contact
    // kite below owns those frames instead. Guns nearby keep the mirror
    // discipline: every tile yielded is paid for in volleys.
    val gunsNear = unit.matchups.threats.exists(t =>
      t.likelyStillThere && t.canAttack(unit) && ! t.unitClass.melee &&
      unit.pixelDistanceEdge(t) * Maff.inv32 <= KiteAway.chaseTiles)
    val meleeChase = unit.matchups.threatsInPixels(KiteAway.chaseTiles * 32.0).exists(t =>
      t.likelyStillThere && t.unitClass.melee &&
      ! FacingAway.dismissMelee(isMelee = true, FacingAway.showingBack(t.angleRadians, t.pixel.radiansTo(unit.pixel))))
    val contactClose = unit.matchups.threatsInPixels(KiteAway.steadyContactTiles * 32.0).exists(_.likelyStillThere)
    // Reach reads, measured once so every leg below agrees: the nearest
    // blade in tiles, and the gun's edge inside its own reach.
    val edgeTiles = unit.pixelRangeMax * Maff.inv32 - KiteAway.kiteEdgeTiles
    val nearTiles = Maff.minBy(
      unit.matchups.threats.view.filter(t => t.likelyStillThere && t.unitClass.melee))(
      t => unit.pixelDistanceEdge(t))
      .map(unit.pixelDistanceEdge(_) * Maff.inv32).getOrElse(Double.PositiveInfinity)
    if (pvpRules && shielded && KiteAway.holdsFire(unit.cooldownLeft, unit.hitPoints, contactClose) && (gunsNear || ! meleeChase)) return false
    // Shared margin read (plant and gate agree by construction): live
    // threats only, turned-away melee dismissed. A blade pressing any
    // mate carves a planted hull, so the plant holds volleys against
    // guns alone - never where melee presses the pack.
    val squadUnits = unit.squad.map(_.units).getOrElse(Seq(unit))
    val marginPixels = KiteAway.enemyMarginTiles * 32.0
    def inMargin(u: FriendlyUnitInfo) =
      u.matchups.threatsInPixels(marginPixels).filter(t =>
        t.likelyStillThere &&
        ! FacingAway.dismissMelee(t.unitClass.melee,
          FacingAway.showingBack(t.angleRadians, t.pixel.radiansTo(u.pixel))))
    val enemyInMargin = squadUnits.exists(u => inMargin(u).nonEmpty)
    val hasMeleeNearby = squadUnits.exists(u => inMargin(u).exists(_.unitClass.melee))
    // Loaded-enough hulls plant the volley instead of entering the kite
    // decision at all: with something hittable inside our reach and the
    // gun inside its window, holding outdamages any ground the yield
    // would buy. Near-dead hulls never plant - only yield. Without guns
    // nearby the plant needs the edge: standing in contact donates the
    // swing for a volley the next cycle fires from range for free.
    if (KiteHold.shouldPlant(unit.cooldownLeft, unit.matchups.targetsInRange.nonEmpty, unit.shieldPoints, unit.unitClass.maxShields, hasMeleeNearby) &&
      (gunsNear || KiteAway.holdsTheEdge(nearTiles, edgeTiles))) {
      plantVolley(unit)

      return true
    }

    // Dying volley first (owner order: shoot every cycle): a near-dead
    // hull with a loaded gun and something hittable spends the shot
    // before running - kiting far past a loaded volley donates the only
    // damage it will ever deal. The escape resumes once the gun cools.
    // Answers through the shared volley verdict, like every other leg.
    if (KiteHold.isNearDead(unit.shieldPoints, unit.unitClass.maxShields) &&
      VolleyDiscipline.shouldVolley(retreating = false, unit.cooldownLeft, unit.matchups.targetsInRange.nonEmpty)) {
      plantVolley(unit)

      return true
    }

    // Owner order (kite while cooling, hold the volley and the edge):
    // melee pressing a ranged hull inside our reach means distance is
    // life - but a gun about to speak holds its volley, and a hull
    // already at the edge holds its ground instead of donating it.
    // Near-dead hulls keep their own flight below; guns alone keep the
    // old ladder; Terran never kites above. Zerg fresh hulls keep
    // their classic hold. A lone blade with no guns nearby presses
    // from the chase radius instead of the margin: the duel is free, so
    // the kite starts before contact instead of after the first hit.
    val meleePressing = ! unit.unitClass.melee && (hasMeleeNearby || (! gunsNear && meleeChase))
    val zergFresh = zergRules && shielded && unit.shieldWound <= KiteAway.zergShieldWound
    if ((pvpRules || zergRules) && ! zergFresh && meleePressing &&
      ! KiteHold.isNearDead(unit.shieldPoints, unit.unitClass.maxShields)) {
      // Loaded volleys plant - without guns nearby only at the edge: a
      // marginal plant donates the swing for a shot the next cycle fires
      // from range for free. The edge plant stands regardless: past the
      // edge the contact cannot reach.
      // Edge plants shoot; anywhere else a loaded gun with no guns around
      // kites first and shoots from the edge instead of trading contact.
      // Holding through the cooldown lets the chaser walk into contact
      // for free, so the walk-back owns cooling frames and the edge owns
      // only the shot itself.
      val edge = KiteAway.holdsTheEdge(nearTiles, edgeTiles)
      val loadedVolley = KiteAway.holdsForVolley(unit.cooldownLeft)
      if ((edge && (gunsNear || loadedVolley)) || (gunsNear && loadedVolley)) {
        plantVolley(unit)

        return true
      }

      kiteAway(unit, combat, hasMeleeNearby = true)

      return true
    }

    if ( ! shouldKite(unit, enemyInMargin, hasMeleeNearby, pvpRules, zergRules, shielded)) return false
    if (holdToShoot(unit, combat)) return true
    // Masonry never chases: kiting away from a building anchor donates
    // DPS and time for nothing, so a building anchor holds instead of
    // yielding. Hold-to-shoot above is untouched: a free shot at a
    // pressing chaser is still taken.
    if (KiteAway.holdAgainstBuildings(nearest.exists(_.unitClass.isBuilding))) return false

    kiteAway(unit, combat, hasMeleeNearby)

    true
  }

  private def kitable(unit: FriendlyUnitInfo): Boolean = {
    if (unit.unitClass.melee && ! unit.isAny(Protoss.Zealot, Protoss.Archon)) return false
    // Blades need no formation to yield (MeleeKiteAway.requiresFormation):
    // melee never holds dress slots, so the gate would only strand blades
    // on formation-less maps. Ranged hulls keep it - their kite is a
    // dress-line maneuver.
    if (MeleeKiteAway.requiresFormation(unit.unitClass.melee) && unit.formationEngage.isEmpty) return false

    true
  }

  private def shouldKite(unit: FriendlyUnitInfo, enemyInMargin: Boolean, hasMeleeNearby: Boolean, pvpRules: Boolean, zergRules: Boolean, shielded: Boolean): Boolean = {
    if (unit.unitClass.melee) return shouldKiteMelee(unit, pvpRules, zergRules, shielded)

    // Healthy hulls with nothing close hold the line: full shields and
    // no live threat inside two tiles means no reason to yield ground,
    // even if a squad mate's margin is hot (owner order). Ranged only;
    // melee answers through the pack gate below.
    if (unit.unitClass.maxShields > 0 && unit.shieldWound == 0 &&
      ! unit.matchups.threatsInPixels(64.0).exists(_.likelyStillThere)) return false

    // Zerg fresh hulls hold: the classic ladder below owns the rest.
    if (zergRules && shielded && unit.shieldWound <= KiteAway.zergShieldWound) return false

    // Fresh hulls hold their loaded volleys, contact or not: at or below
    // the fresh wound the gun outweighs the yield, so a loaded gun never
    // yields without real shield damage AND blades in contact. A cooling
    // gun holds no volley - standing through the cooldown donates the
    // contact for nothing, so only a loaded gun may hold. PvP-only, and
    // only where shields exist to read: other matchups run the ladder
    // untouched.
    if (pvpRules && shielded) {
      val meleeClose = unit.matchups.threatsInPixels(KiteAway.meleeCloseTiles * 32.0).exists(t =>
        t.likelyStillThere && t.unitClass.melee)
      if ( ! KiteAway.shouldKite(unit.shieldWound, meleeClose) && KiteAway.holdsForVolley(unit.cooldownLeft)) return false
    }

    // Near-dead hulls kite on melee contact alone: collapsed shields with
    // blades on top mean the next volley kills, so the wound ladder below
    // never gets to hold them for shots they never live to fire. Guns
    // alone cannot be outrun, so the hull holds its last volleys instead.
    if (KiteHold.shouldFleeNearDead(unit.shieldPoints, unit.unitClass.maxShields, enemyInMargin, hasMeleeNearby)) return true
    if ( ! KiteAway.shouldRetreat(unit.totalHealth, unit.cooldownLeft, enemyInMargin, hasMeleeNearby)) return false

    true
  }

  /**
   * Melee kite gate (thin seam over the pure kernel): blades yield
   * only on the kernel's terms - nearby lings, a superior zealot pack,
   * or a dying hull still swinging at melee. Anything else stands.
   */
  private def shouldKiteMelee(unit: FriendlyUnitInfo, pvpRules: Boolean, zergRules: Boolean, shielded: Boolean): Boolean = {
    // Zerg fresh hulls hold: the classic ladder below owns the rest.
    if (zergRules && shielded && unit.shieldWound <= KiteAway.zergShieldWound) return false

    if (pvpRules) {
      // Calm healthy-enough blades hold: not hit in forty frames with
      // 41+ HP means no pressure worth yielding to - stand and swing.
      if (MeleeKiteAway.holdsWhenCalm(With.framesSince(unit.lastFrameTakingDamage), unit.totalHealth)) return false

      // Pack-average hold: blades whose mates average less HP hold the
      // frame instead of kiting - the healthier hull stands while the
      // pack chews together.
      val packMates = unit.matchups.allies.view.filter(a =>
        a.alive && Protoss.Zealot(a) && a.id != unit.id && unit.pixelDistanceEdge(a) <= MeleeKiteAway.packMateTiles * 32.0)
      if (MeleeKiteAway.standsWithWeakerPack(packMates.map(_.totalHealth).sum, packMates.size, unit.totalHealth)) return false
    }

    val marginPixels = KiteAway.enemyMarginTiles * 32
    // Live blades only: ghost zealots must not inflate the pack count.
    val liveMargin = unit.matchups.threatsInPixels(marginPixels).filter(_.likelyStillThere)
    val foeZealots = liveMargin.count(Protoss.Zealot)
    val mates = unit.matchups.allies.count(a =>
      a.alive && a.unitClass.isWarrior && a.pixelDistanceEdge(unit) <= marginPixels)

    MeleeKiteAway.shouldRetreat(
      totalHp = unit.totalHealth,
      coolingDown = unit.cooldownLeft > 0,
      hasSlowLingNearby = liveMargin.exists(IsSlowling),
      hasSpeedlingNearby = liveMargin.exists(IsSpeedling),
      packIntact = Maff.minBy(unit.matchups.allies.view.filter(a =>
        a.alive && Protoss.Zealot(a) && a.id != unit.id))(_.pixelDistanceCenter(unit.pixel))
        .exists(mate => unit.pixelDistanceCenter(mate.pixel) * Maff.inv32 <= MeleeKiteAway.packKiteTiles),
      outnumberedByZealots = foeZealots > mates + 1,
      attackingMelee = unit.matchups.targetNearest.exists(_.unitClass.melee))
  }

  // Hold-to-shoot: a committed chaser met with a ready gun is a free shot,
  // not a reason to yield ground. Fires only when we'd otherwise kite, and
  // never steals another duel's target: the chaser must be ours or none.
  // Vultures duel on their own rule: nearly-ready guns fire in range, and
  // head-on sprinters meet the shot at bonus reach - kiting a Vulture
  // donates the exact distance its speed wants.
  private def holdingShot(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    // Live chasers only: holding against a fogged ghost stands the hull
    // down for nothing while the real presser walks in for free.
    val chaser = Maff.minBy(unit.matchups.threats.view.filter(_.likelyStillThere))(_.pixelDistanceEdge(unit))
    chaser.exists(c => combat.target.forall(_.id == c.id) && holdsAgainst(unit, c))
  }

  private def holdsAgainst(unit: FriendlyUnitInfo, chaser: UnitInfo): Boolean = {
    if (Terran.Vulture(chaser)) {
      // Our reach, never the chaser's: at 4-5 tiles a Dragoon cannot
      // reply to a Vulture, so measuring "in range" against the
      // Vulture's five tiles pins a hold with a silent gun - the unit
      // stands inside enemy range instead of kiting. Potshot reads the
      // same duel off our range; the hold must agree with it.
      HoldToShoot.shouldStandVsVulture(
        unit.pixelDistanceEdge(chaser),
        unit.pixelRangeAgainst(chaser),
        unit.cooldownLeft,
        With.latency.latencyFrames,
        chaser.speedApproaching(unit.pixel),
        FacingAway.facingUs(chaser.angleRadians, chaser.pixel.radiansTo(unit.pixel)))
    } else {
      HoldToShoot.shouldStand(
        unit.pixelDistanceEdge(chaser),
        HoldToShoot.effectiveRange(chaser.pixelRangeAgainst(unit), chaser.topSpeed, windupFrames = 0, unit.topSpeed * unit.unitClass.accelerationFrames / 2.0),
        unit.cooldownLeft,
        With.latency.latencyFrames,
        chaser.speedApproaching(unit.pixel),
        HoldToShoot.holdMarginFrames)
    }
  }

  private def holdToShoot(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! holdingShot(unit, combat)) return false

    unit.agent.act("HoldToShoot")
    unit.agent.decision.set(unit.pixel)
    Commander.hold(unit)

    true
  }

  private def plantVolley(unit: FriendlyUnitInfo): Unit = {
    unit.agent.act("KiteHold")
    unit.agent.decision.set(unit.pixel)
    Commander.hold(unit)
  }

  /**
   * Contact floor (owner order, 1Drag_v_1Zeal post-mortem): under blades
   * the yield never drops below the paced step. A fresh wound want is
   * ~zero, and a zero step is standing - the paced step keeps the hull
   * ahead of the chase while still landing every cooldown cycle.
   */
  private def contactYield(unit: FriendlyUnitInfo, woundWant: Double): Double = {
    val meleeContact = unit.matchups.threatsInPixels(KiteAway.steadyContactTiles * 32.0).exists(t =>
      t.likelyStillThere && t.unitClass.melee)
    // Walk-back inside the cooldown, wound-independent: the paced cap
    // takes min() with the wound want, so a fresh hull paces zero and
    // the floor below it lifts nothing. Contact needs the raw walk-back
    // (shoot every cycle, spend cooling frames on distance), floored by
    // the wound want above it, never capped by it.
    val walkback = Math.max(0, unit.cooldownLeft - (With.latency.latencyFrames + 2)) * unit.topSpeed
    if (meleeContact) KiteAway.contactYield(woundWant, walkback) else woundWant
  }

  /**
   * Edge-capped contact step (owner order: never walk out of range):
   * the contact floor can overshoot the gun's own reach, so the step
   * stops at max range minus the edge margin instead. A hull already
   * there holds (the gate above), so this only trims the stride.
   */
  private def edgedYield(unit: FriendlyUnitInfo, woundWant: Double): Double = {
    val foe = unit.matchups.targetNearest.get
    KiteAway.edgeCappedYield(
      contactYield(unit, woundWant),
      unit.pixelDistanceEdge(foe) * Maff.inv32,
      unit.pixelRangeMax * Maff.inv32,
      KiteAway.kiteEdgeTiles)
  }

  private def kiteAway(unit: FriendlyUnitInfo, combat: Combat, hasMeleeNearby: Boolean): Unit = {
    unit.agent.act("KiteAway")
    // A commanded withdrawal is not stuckness: the stuck detector reads
    // a loaded, in-range, non-firing gun as failure and would stop the
    // hull mid-kite - exactly the stutter that lets chasers close for
    // free. Withholding fire on purpose is the doctrine, not a stall.
    unit.resetSticking()
    // Yield from the feet, never from the pin: a static formation slot
    // brakes the fallback every stride (arrive, guard, re-issue), and
    // the stutter lets chasers close for free - the loss pattern on
    // lone duels. A receding anchor never arrives, so the hull cruises
    // instead of stopping; the dress leg reclaims it when the gate
    // closes, so the line still gets its hull back.
    val anchor = unit.pixel
    // Only yield what the cooldown forces against guns alone: the step
    // never exceeds the walk-back inside the remaining cooldown, so the
    // return trip stops eating the next volley. Any blade pressure on
    // the pack keeps the full wound want instead - in a mixed furball
    // the short step optimizes the ranged sub-duel but donates blade
    // contact, and distance is life against contact. Melee on top of a
    // near-dead hull goes far instead - blades carve what stands, and
    // the wound want understates the exit a dying hull needs.
    val woundWant = WoundMargin(0.0, KiteAway.maxOffsetTiles).pixels(unit.woundPercent())
    val meleeNear = unit.matchups.threatsInPixels(KiteAway.enemyMarginTiles * 32.0).exists(t => t.likelyStillThere && t.unitClass.melee)
    val nearDeadFar = KiteHold.isNearDead(unit.shieldPoints, unit.unitClass.maxShields) && meleeNear
    // No guns nearby, blades on top: a loaded hull still steps out
    // instead of planting into the swing - the gap reopens and the next
    // volley fires from range for free. Guns nearby keep the mirror
    // discipline: every tile yielded is paid for in volleys.
    val gunsNear = unit.matchups.threats.exists(t =>
      t.likelyStillThere && t.canAttack(unit) && ! t.unitClass.melee &&
      unit.pixelDistanceEdge(t) * Maff.inv32 <= KiteAway.chaseTiles)
    val meleeContact = unit.matchups.threatsInPixels(KiteAway.steadyContactTiles * 32.0).exists(t => t.likelyStillThere && t.unitClass.melee)
    val offsetPixels = if (nearDeadFar) KiteHold.nearDeadOffsetTiles * 32.0
      else if (hasMeleeNearby) {
        val edged = edgedYield(unit, woundWant)
        if (! gunsNear && meleeContact) KiteAway.escapeYield(edged, KiteAway.meleeEscapeTiles) else edged
      }
      else KiteYield.offsetPixels(woundWant, unit.cooldownLeft, unit.topSpeed, With.latency.latencyFrames + 2)
    val away = anchor.project(unit.matchups.targetNearest.get.pixel, -offsetPixels).walkablePixel
    // The leash stays in the gun-free duel too: dragging the pack into a
    // gunfight breaks the focus the leash exists to keep. Committed
    // strides cruise instead of braking into every hop: short steps reset
    // acceleration faster than the gap opens, so the yield strides past
    // its point along the same ray and the direction still comes from
    // the duel.
    val leashedAway = if (gunsNear) away else leashed(unit, unit.matchups.targetNearest.get, away)
    // Committed strides cruise instead of braking into every hop, but
    // only where no guns watch: the mirror paces its volleys inside its
    // own reach, and dying hulls keep their far flight.
    val committed =
      if ( ! gunsNear && hasMeleeNearby && ! nearDeadFar) {
        val far = unit.pixel.project(leashedAway, KiteAway.kiteCommitTiles * 32.0)
        if (far.walkable) far else leashedAway.walkablePixel
      } else leashedAway
    // Steady the course: re-deriving the heading every frame from a
    // jittering chaser turns forever instead of cruising, so a held
    // heading stands until it lapses or the duel turns past the cone.
    // Lone blades only: the mirror stays twitchy on purpose. New games
    // restart the frame counter, which releases every hold.
    val freshHeading = unit.pixel.radiansTo(committed)
    val steadied = steadyHeadings.get(unit.id) match {
      case Some((held, heldFrame)) if ! gunsNear && hasMeleeNearby && ! nearDeadFar &&
        ! KiteAway.shouldRefreshHeading(heldFrame, With.frame) &&
        KiteAway.holdsHeading(freshHeading, held, OrderContinue.headingConeRadians) =>
        unit.pixel.radiateRadians(held, KiteAway.kiteCommitTiles * 32.0).walkablePixel
      case _ =>
        if ( ! gunsNear && hasMeleeNearby && ! nearDeadFar) steadyHeadings(unit.id) = (freshHeading, With.frame)
        committed
    }
    unit.agent.decision.set(steadied)
    // Contact kites interrupt the firing solution explicitly: a standing
    // attack order on a live in-range target vetoes every move, so the
    // gate-proven yield (cooling hull, blades on top) would pose forever
    // behind its own volley. Loaded guns never reach this leg, so the
    // interrupt spends no volleys - it only spends cooling frames.
    if (meleeContact) Commander.moveOverriding(unit)
    else combat.maneuvers.move()
  }

  /**
   * Pack leash (owner order): a kiter that ran more than six tiles from
   * its nearest friendly warrior steers back toward the pack instead of
   * across the map - but never closer to the chaser than it stands, and
   * never on a heading that turns it back into the chase. No warriors
   * out, no leash: a lone hull kites free. Near-dead hulls keep their
   * far flight instead of dying among friends. The return lands on the
   * six-tile ring, never at the friend's feet: the yield keeps its
   * distance while the pack stays in sharing range.
   */
  private def leashed(unit: FriendlyUnitInfo, foe: UnitInfo, away: Mathematics.Points.Pixel): Mathematics.Points.Pixel = {
    if (KiteHold.isNearDead(unit.shieldPoints, unit.unitClass.maxShields)) return away

    Maff.minBy(unit.matchups.allies.view.filter(a =>
      a.alive && a.id != unit.id && a.unitClass.isWarrior))(unit.pixelDistanceEdge(_))
      .filter(f => KiteAway.shouldLeash(
        unit.pixelDistanceEdge(f) * Maff.inv32,
        KiteAway.leashTiles,
        f.pixelDistanceEdge(foe),
        unit.pixelDistanceEdge(foe),
        KiteAway.alignedAway(unit.pixel, f.pixel, foe.pixel)))
      .map(f => {
        val over = f.pixel.pixelDistance(away) - KiteAway.leashTiles * 32.0
        if (over <= 0) away else away.project(f.pixel, over).walkablePixel
      })
      .getOrElse(away)
  }

  // Static standoff: an idle unit standing inside a defensive building's
  // reach while the cluster takes fire steps out to range + margin + bonus
  // instead of absorbing hits. Fighting units (targets in range) and
  // committed pushes (shouldFight) are untouched; the attack window lapses
  // once fire stops, so the squad resumes on its own (hysteresis).
  private def staticStandoff(unit: FriendlyUnitInfo, combat: Combat): Boolean = {
    if ( ! standoffHot(unit)) return false

    val threat = Maff.minBy(unit.matchups.threats.filter(_.unitClass.isStaticDefense))(_.pixelDistanceEdge(unit))
    if (threat.isEmpty) return false

    val gap = standoffGap(unit, threat.get)
    if (gap <= 0) return false

    stepOut(unit, combat, threat.get, gap)

    true
  }

  private def standoffHot(unit: FriendlyUnitInfo): Boolean = {
    if (unit.agent.shouldFight) return false

    if (unit.matchups.targetsInRange.nonEmpty) return false

    val squadUnits = unit.squad.map(_.units).getOrElse(Seq(unit))
    val clusterHot = squadUnits.exists(u => ClusterSafety.recentlyAttacked(With.framesSince(u.lastFrameTakingDamage)))
    if ( ! clusterHot) return false

    true
  }

  private def standoffGap(unit: FriendlyUnitInfo, threat: UnitInfo): Double =
    ClusterSafety.standoffGapPixels(
      unit.pixelDistanceEdge(threat),
      threat.pixelRangeAgainst(unit),
      KiteAway.enemyMarginTiles,
      clusterAttacked = true)

  private def stepOut(unit: FriendlyUnitInfo, combat: Combat, threat: UnitInfo, gap: Double): Unit = {
    unit.agent.act("StaticStandoff")
    unit.agent.decision.set(unit.pixel.project(threat.pixel, -gap).walkablePixel)
    combat.maneuvers.move()
  }
}
