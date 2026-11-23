package Micro.Actions.Protoss.Dragoon

import Micro.Actions.UnitControl
import Debugging.Visualizations.Forces
import Lifecycle.With
import Macro.Facts.MacroFacts
import Mathematics.Maff
import Micro.Actions.Action
import Micro.Actions.Combat.Decisioning.Kiting.{FacingAway, KiteAway, KiteHold}
import Micro.Actions.Combat.Decisioning.VolleyDiscipline
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import Micro.Heuristics.Potential
import Micro.Targeting.Target
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.Time.Minutes

object BeDragoonVsSlowlot extends Action {
  override def appliesToUnit(unit: FriendlyUnitInfo): Boolean = Protoss.Dragoon(unit)

  override def allowed(unit: FriendlyUnitInfo): Boolean = (
    Protoss.Dragoon(unit)
    && With.frame < Minutes(7)()
    && unit.matchups.threats.forall(_.isAny(Protoss.Probe, Protoss.Zealot))
    && ! MacroFacts.enemyHasUpgrade(Protoss.ZealotSpeed))

  override protected def perform(unit: FriendlyUnitInfo): UnitControl = {
    // The W1 duel owns losing duels: where the shipped kite kernel fires,
    // this legacy dance stands down without issuing (and without sleeping),
    // so the technique runs on a ready unit and kites on cooldown instead
    // of being starved by our own margin moves. Same read as
    // KiteAwayBehavior.shouldKite (live threats in margin, turned-away
    // melee dismissed); both paths agree by construction.
    if (yieldsToKite(unit)) return UnitControl.Declined

    // Spend loaded volleys before footwork: a loaded gun with something
    // hittable outdamages margin positioning on any hull, and the
    // ledger counts every loaded move past a hittable target as waste.
    // The plant forbids standing under blades for survival; this rung
    // only spends the shot the engine confirms, never holds the hull.
    // Answers through the shared volley verdict - one question, one owner.
    if (VolleyDiscipline.shouldVolley(retreating = false, unit.cooldownLeft, unit.matchups.targetsInRange.nonEmpty)) {
      Target.choose(unit)
      if (unit.agent.toAttack.isDefined && unit.readyForAttackOrder(unit.agent.toAttack.get)) {
        Commander.attack(unit)

        return UnitControl.claimedIf(! unit.ready)
      }
      // Loaded but not ready (aiming, turning): hold the volley instead
      // of footworking past it - the gun speaks the moment it aims,
      // and no step is worth a donated shot.
      Commander.hold(unit)

      return UnitControl.claimedIf(! unit.ready)
    }

    // Opening stand owns the frame (never the duel): no margin
    // footwork through an unformed line. The rung above already shot
    // what it could; contact hands back to the duel.
    if (Tactic.Squads.MarchOrders.openingHold(unit)) return UnitControl.claimedIf(! unit.ready)

    val margin    = Maff.min(unit.matchups.threatsInPixels(160).map(t => t.pixelsToGetInRange(unit) + 32 - Maff.or0(16, t.presumptiveTarget.contains(unit)) - 16 * t.dot(unit.pixel))).getOrElse(320d).toInt
    val safety    = DragoonSlowlotPolicy.safety(margin, unit.totalHealth)

    unit.agent.escalatePriority(DragoonSlowlotPolicy.priorityForSafety(safety))

    val marginDelta = DragoonSlowlotPolicy.marginDeltaPixels(margin, unit.totalHealth)

    unit.agent.forces.put(Forces.spacing,  Potential.preferSpacing(unit))
    unit.agent.forces.put(Forces.pushing,  Potential.followPushes(unit))
    unit.agent.forces.put(Forces.threat,   Potential.hardAvoidThreatRange(unit, Math.max(32, marginDelta)))

    if (marginDelta >= 0) {
      Target.choose(unit)
      if (unit.agent.toAttack.isDefined) {
        if (unit.readyForAttackOrder(unit.agent.toAttack.get)) {
          Commander.attack(unit)
        } else if (marginDelta > 32 && ! unit.matchups.threatsInPixels(160).exists(_.presumptiveTarget.contains(unit))) {
          unit.agent.forces.put(Forces.target, Potential.towards(unit, unit.pixelToFireAtExhaustive(unit.agent.toAttack.get)))
          unit.agent.forceTo(MicroPathing.getWaypointInDirection(unit, unit.agent.forces.sum.radians).orElse(unit.agent.destinationNext.pixel))
          Commander.move(unit)
        } else if (unit.pixelsToGetInRange(unit.agent.toAttack.get) < -32) {
          retreat(unit)
        } else {
          Commander.attack(unit)
        }
      }
      // Allow default movement to take over
    } else {
      retreat(unit)
    }
    UnitControl.claimedIf(! unit.ready)
  }

  def retreat(unit: FriendlyUnitInfo): Unit = {
    val force = unit.agent.forces.sum.radians
    lazy val tilePath = MicroPathing.getThreatAwarePath(unit, preferHome = false)
    val waypoint = MicroPathing.bestWaypoints(unit, force, None, simpleSafety = true, tilePath, adjustPath = true)
      .headOption.map(_._1)

    waypoint.foreach(p => {
      unit.agent.decision.set(p)
      Commander.move(unit)
    })
  }

  /**
   * Yield gate: the shipped kite kernel owns losing frames. Same read as
   * KiteAwayBehavior.shouldKite for a ranged hull - live threats in the
   * margin, turned-away melee dismissed - so the yield and the duel leg
   * fire on the same frames by construction. Yielding issues nothing, so
   * the technique runs on a ready unit instead of being slept out by our
   * own margin moves.
   */
  private def yieldsToKite(unit: FriendlyUnitInfo): Boolean = {
    val enemyIsProtoss = With.enemies.exists(_.isProtoss)
    val enemyIsZerg = With.enemies.exists(_.isZerg)
    val enemyIsTerran = With.enemies.exists(_.isTerran)
    // Terran never kites, on both paths: the dance never stands down
    // for a kite that will not fire, and the technique never runs one.
    if (enemyIsTerran && ! enemyIsProtoss && ! enemyIsZerg) return false
    // ShieldWound gates need shields to read: shieldless hulls always
    // read zero, so the refinements below would pin them standing.
    val shielded = unit.unitClass.maxShields > 0

    val zergRules = enemyIsZerg && ! enemyIsProtoss
    val pvpRules = enemyIsProtoss
    val marginPixels = KiteAway.enemyMarginTiles * 32.0
    def inMargin(u: FriendlyUnitInfo) =
      u.matchups.threatsInPixels(marginPixels).filter(t =>
        t.likelyStillThere &&
        ! FacingAway.dismissMelee(t.unitClass.melee,
          FacingAway.showingBack(t.angleRadians, t.pixel.radiansTo(u.pixel))))
    // Shared pack read (behavior gate reads the squad too): a hull
    // whose mate is pressed kites with the pack instead of footworking
    // through its own duel - one writer per frame on pack maps.
    val squadMargin = unit.squad.map(_.units).getOrElse(Seq(unit)).view.flatMap(inMargin).toSeq
    def hasMeleeInMargin: Boolean = squadMargin.exists(_.unitClass.melee)
    // Zerg fresh hulls hold on both paths: the classic ladder owns the rest.
    if (zergRules && shielded && unit.shieldWound <= KiteAway.zergShieldWound) return false
    // Fresh, loaded and clear hulls hold their volleys on both paths
    // (PvP-only): the kite donated the shooting, so the dance never
    // stands down for a kite that will not fire.
    val contactClose = unit.matchups.threatsInPixels(KiteAway.steadyContactTiles * 32.0).exists(_.likelyStillThere)
    if (pvpRules && shielded && KiteAway.holdsFire(unit.cooldownLeft, unit.hitPoints, contactClose)) return false

    // Plant owns the frame where it would fire: the dance stands down
    // instead of starving the hold - a loaded volley with something
    // hittable outdamages margin footwork on both paths.
    if (KiteHold.shouldPlant(unit.cooldownLeft, unit.matchups.targetsInRange.nonEmpty, unit.shieldPoints, unit.unitClass.maxShields, hasMeleeNearby = hasMeleeInMargin)) return true

    // Contact kite on both paths (PvP): cooling with melee inside 2.7
    // tiles means the behavior yields real ground - the dance stands
    // down instead of footworking past the volley.
    val meleeContact = unit.matchups.threatsInPixels(KiteAway.steadyContactTiles * 32.0).exists(t =>
      t.likelyStillThere && t.unitClass.melee)
    if (pvpRules && unit.cooldownLeft > 0 && meleeContact) return true

    // The dance stands down only where the kite would fire: fresh hulls
    // and ranged-only pressure hold their volleys on both paths (PvP),
    // so the yield mirrors the behavior gate exactly.
    if (pvpRules && shielded) {
      val meleeClose = unit.matchups.threatsInPixels(KiteAway.meleeCloseTiles * 32.0).exists(t =>
        t.likelyStillThere && t.unitClass.melee)
      if ( ! KiteAway.shouldKite(unit.shieldWound, meleeClose)) return false
    }

    // Near-dead hulls yield past the wound ladder too where the shipped
    // kite kites them: collapsed shields with blades on top. Guns alone
    // hold their last volleys on both paths - neither donates them.
    if (KiteHold.shouldFleeNearDead(unit.shieldPoints, unit.unitClass.maxShields, enemyInMargin = squadMargin.nonEmpty, hasMeleeNearby = hasMeleeInMargin)) return true

    KiteAway.shouldRetreat(
      unit.totalHealth,
      unit.cooldownLeft,
      enemyInMargin = squadMargin.nonEmpty,
      hasMeleeNearby = hasMeleeInMargin)
  }
}
