package Micro.Actions.Combat.Maneuvering

import Micro.Actions.UnitControl
import Debugging.Visualizations.Forces
import Lifecycle.With
import Mathematics.Points.Pixel
import Mathematics.{Maff, Shapes}
import Micro.Actions.Action
import Micro.Actions.Combat.Decisioning.Kiting.KiteAway
import Micro.Agency.Commander
import Micro.Coordination.Pathing.MicroPathing
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.?
import Utilities.Time.Minutes
import Utilities.UnitFilters.{IsTank, IsWorker}

object Retreat extends Action {

  /** Never brake into arrival while fleeing: a destination inside this,
    * in tiles, gets pushed past itself, so the unit keeps running
    * instead of stopping under fire to re-decide. Precision maneuvers
    * (boarding, formation offsets) opt out and arrive exactly. */
  val arrivalPushTiles: Double = 2.0

  /** How far past the stale destination the push lands, in tiles. */
  val arrivalPushPastTiles: Double = 4.0

  def pushPastArrival(distTiles: Double, preciseArrival: Boolean): Boolean =
    ! preciseArrival && distTiles < arrivalPushTiles
  
  /** Full retreat planner: picks WHERE a fleeing unit runs (board, shotgun, scatter, path), never WHETHER it flees - that verdict belongs to FightRules. */
  override def allowed(unit: FriendlyUnitInfo): Boolean = unit.canMove && unit.matchups.threats.nonEmpty

  case class RetreatPlan(unit: FriendlyUnitInfo, to: Pixel, name: String, preciseArrival: Boolean = false) {
    def apply(unit: FriendlyUnitInfo): Boolean = {
      if (unit.ready) {
        unit.agent.toAttack = None // Affects traffic prioritization
        // Never brake into arrival while fleeing: a destination inside
        // two tiles gets pushed past itself, so the unit keeps running
        // instead of stopping under fire to re-decide. Precision maneuvers
        // (boarding, formation offsets) opt out and arrive exactly.
        val distTiles = unit.pixelDistanceCenter(to) * Maff.inv32
        val runTo = ?(pushPastArrival(distTiles, preciseArrival),
          unit.pixel.project(to, unit.pixelDistanceCenter(to) + arrivalPushPastTiles * 32).walkablePixel,
          to)
        unit.agent.decision.set(runTo)
        if (With.configuration.debugging) {
          unit.agent.act("Retreat" + name + "(" + Tactic.Squads.Decisions.RetreatVerdict.retreatLabel(
            unit.agent.fightReason,
            With.battles.globalAttack.us.skimStrengthUnits,
            With.battles.globalAttack.enemy.skimStrengthUnits) + ")")
        }
        // Fleeing scouts reorder on a wide deadband: their waypoints hop
        // with the threat, and tight refresh restarts movement every frame.
        Commander.move(unit, ?(unit.agent.isScout, 160.0, 32.0))
      }
      unit.unready
    }
  }

  override def perform(unit: FriendlyUnitInfo): UnitControl = {
    // Siege shells outrank every retreat plan: a detected ground unit
    // that does not want the tank battle steps past the shell line
    // first, waypoints second. Hulls with a battle verdict keep the
    // TankHug discipline below instead.
    if (ArtilleryKeepOff.act(unit)) return UnitControl.Claimed

    getRetreat(unit)(unit)
    UnitControl.claimedIf(! unit.ready)
  }

  def getRetreat(unit: FriendlyUnitInfo): RetreatPlan = {
    // Decide our goals in retreating
    def timeOriginOfThreat(threat: UnitInfo): Double = threat.framesToGetInRange(unit, unit.agent.safety)
    lazy val distanceOriginUs     = unit.pixelDistanceTravelling(unit.agent.safety)
    lazy val distanceOriginEnemy  = Maff.min(unit.matchups.threats.view.map(t => t.pixelDistanceTravelling(unit.agent.safety) - t.pixelRangeAgainst(unit))).getOrElse(2.0 * With.mapPixelWidth)
    lazy val enemyCloser          = distanceOriginUs + 160 >= distanceOriginEnemy
    lazy val timeOriginUs         = unit.framesToTravelTo(unit.agent.safety)
    lazy val timeOriginEnemy      = Maff.takePercentile(0.1, unit.matchups.threats)(Ordering.by(timeOriginOfThreat)).map(timeOriginOfThreat).headOption.getOrElse(Double.PositiveInfinity)
    lazy val enemySooner          = timeOriginUs + 96 >= timeOriginEnemy
    lazy val enemySieging         = unit.matchups.enemies.exists(_.isAny(IsTank, Zerg.Lurker)) && ! unit.base.exists(_.owner.isEnemy)
    lazy val goalSidestep         = Protoss.DarkTemplar(unit) || (enemySieging && ! enemyCloser && ! enemySooner)
    lazy val safetyIsSafe         = unit.agent.safety.tile.enemyRangeAgainst(unit) < Math.max(1, unit.tile.enemyRangeAgainst(unit))
    lazy val goalCircle           = unit.matchups.threats.forall(unit.effectiveRangePixels > _.effectiveRangePixels + 32) && unit.zone == unit.agent.defaultHome.zone
    lazy val goalReturn           = ! unit.agent.isScout && ! goalSidestep && (unit.zone == unit.agent.safety.zone && unit.pixelDistanceCenter(unit.agent.safety) >= 32 * unit.agent.safety.tile.enemyRangeAgainst(unit) && Shapes.Ray(unit.pixel, unit.agent.safety).forall(_.tile.enemyRangeAgainst(unit) <= unit.tile.enemyRangeAgainst(unit)))
    lazy val goalHome             = ! unit.agent.isScout && ! goalSidestep && (unit.zone != unit.agent.safety.zone && (enemyCloser || enemySooner))
    lazy val goalOrigin           = goalReturn || goalHome
    lazy val goalSafety           = ! unit.matchups.withinSafetyMargin
    lazy val forceVector = {
      if (unit.agent.forces.isEmpty) {
        MicroPathing.setDefaultForces(unit, goalOrigin = goalOrigin, goalSafety = goalSafety)
      }
      if (unit.base.exists(_.isOurMain) && unit.isAny(Terran.Marine, Terran.Vulture, Protoss.Dragoon) && unit.matchups.threats.forall(_.isAny(IsWorker, Protoss.Zealot, Protoss.DarkTemplar, Zerg.Zergling))) {
        unit.agent.forces(Forces.travel) *= 0.1
      }
      unit.agent.forces.sum
    }
    lazy val force = forceVector.radians

    // Board when the ride is a step away (engine pickup radius is 32 px edge-to-edge).
    if ( ! unit.agent.shouldFight && unit.agent.ride.exists(_.pixelDistanceEdge(unit) <= 32 + 32) && unit.matchups.targetsInRange.isEmpty) {
      Commander.rightClick(unit, unit.agent.ride.get)

      return RetreatPlan(unit, unit.agent.ride.map(_.pixel).getOrElse(unit.pixel), "Hail", preciseArrival = true)
    }

    // Against melee rush: Retreat directly to heart so workers can help.
    // Baseless maps (UMS duels) have no heart and no workers: ourMetro
    // has no answer there, so the branch stays shut instead of crashing.
    if (With.frame < Minutes(6)() && With.geography.ourBases.nonEmpty && unit.isAny(Terran.Marine, Protoss.Zealot) && unit.squad.forall(_.units.size < 5) && unit.metro.contains(With.geography.ourMetro) && unit.matchups.threats.forall(_.pixelRangeAgainst(unit) < 64)) {
      val baseToRunTo = Maff.orElse(With.geography.ourBases, Seq(With.geography.ourMain)).minBy(_.heart.groundTiles(With.scouting.enemyThreatOrigin))

      return RetreatPlan(unit, baseToRunTo.heart.center, "Run")
    }

    // Enshuttled VIPs: If not primary passenger, retreat towards primary passenger
    val leadPassenger = unit.agent.ride.flatMap(_.agent.passengersPrioritized.headOption)
    if (leadPassenger.isDefined && ! leadPassenger.contains(unit)) {
      return RetreatPlan(unit, leadPassenger.get.pixel.add(forceVector.normalize(32).toPoint.asPixel.walkablePixel), "Shotgun", preciseArrival = true)
    }

    // Corsairs outrun ground fire locally instead of running home: scatter
    // away from the deepest threat by 10 tiles, randomized by +-4 (per-frame
    // re-rolls stay smooth: the point stays many frames of travel away along
    // the threat ray). Near-death Corsairs keep the safety waypoints below,
    // and so does any retreat with air threats: Mutalisks and Scourge run
    // a scattering Corsair down anywhere, so those always run for base.
    if (Protoss.Corsair(unit)
      && ! KiteAway.isCriticallyWounded(unit.totalHealth)
      && unit.matchups.threats.nonEmpty
      && unit.matchups.threats.forall(! _.flying)) {
      val distancePixels = (10.0 + 8.0 * scala.util.Random.nextDouble() - 4.0) * 32.0
      val away = unit.matchups.threatDeepest
        .map(threat => unit.pixel.project(threat.pixel, -distancePixels).clamp())
        .getOrElse(unit.agent.safety)

      return RetreatPlan(unit, away, "Scatter")
    }

    lazy val tilePath         = MicroPathing.getThreatAwarePath(unit, preferHome = goalOrigin)
    lazy val waypointOrigin   = (unit.agent.safety, "Origin")
    def legName(leg: MicroPathing.WaypointLeg): String = leg match {
      case MicroPathing.WaypointSimple => "Simple"
      case MicroPathing.WaypointPath   => "Path"
      case MicroPathing.WaypointForces => "Force"
    }
    // Large land units wedge between fields and die there, so their flee
    // spots stay 1.2 tiles clear of minerals and geysers (one shared
    // rule, not a second predicate next to FleeMineralClearance).
    def resourcesNear(pixel: Pixel): Seq[Pixel] =
      pixel.tile.adjacent9.view.flatMap(_.units).filter(u => u.unitClass.isMinerals || u.unitClass.isGas).map(_.pixel).toSeq
    def clearOfResources(pixel: Pixel): Boolean =
      FleeMineralClearance.clearOfResources(pixel, resourcesNear(pixel))
    val largeFleer = FleeMineralClearance.applies(unit.unitClass.size == ProxyBwapi.Size.Large, unit.flying)
    val waypoint = ?(largeFleer,
      MicroPathing.bestWaypoints(unit, force, ?(goalOrigin, Some(unit.agent.safety), None), goalSafety && safetyIsSafe, tilePath, adjustPath = true)
        .find(w => clearOfResources(w._1)).map { case (p, leg) => (p, legName(leg)) }.getOrElse(waypointOrigin),
      MicroPathing.bestWaypoints(unit, force, ?(goalOrigin, Some(unit.agent.safety), None), goalSafety && safetyIsSafe, tilePath, adjustPath = true)
        .headOption.map { case (p, leg) => (p, legName(leg)) }.getOrElse(waypointOrigin))
    RetreatPlan(unit, waypoint._1, waypoint._2)
  }
}

