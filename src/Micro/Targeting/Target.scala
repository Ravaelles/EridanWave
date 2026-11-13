package Micro.Targeting

import Lifecycle.With
import Mathematics.Maff
import Micro.Targeting.FiltersRequired.{TargetFilterAntiAir, TargetFilterCanAttack, TargetFilterEnemy, TargetFilterFocus, TargetFilterMissing, TargetFilterOverlord, TargetFilterReaver, TargetFilterRush, TargetFilterScourge, TargetFilterStayCloaked, TargetFilterType, TargetFilterVsTank, TargetFilterVulture}
import Micro.Targeting.FiltersSituational.TargetFilterWhitelist
import ProxyBwapi.Orders
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.UnitFilters.IsWorker
import Utilities.?

object Target {

  val defaultFilters: Vector[TargetFilter] = Vector(
    TargetFilterEnemy,
    TargetFilterCanAttack,
    TargetFilterMissing,
    TargetFilterType,
    TargetFilterFocus,
    TargetFilterStayCloaked,
    TargetFilterScourge,
    TargetFilterAntiAir,
    TargetFilterOverlord,
    TargetFilterReaver,
    TargetFilterRush,
    TargetFilterVulture,
    TargetFilterVsTank)

  def choose(attacker: FriendlyUnitInfo, required: TargetFilter*): Option[UnitInfo] = {
    attacker.agent.toAttack = best(attacker, required: _*)
    attacker.agent.toAttack
  }

  private def hasCombatPriority(unit: UnitInfo): Boolean = {
    unit.unitClass.attacksOrCastsOrDetectsOrTransports
  }
  private def acceleratesDemise(attacker: UnitInfo, target: UnitInfo): Boolean = {
    target.doomFrameAbsolute > With.frame + attacker.framesToConnectDamage(target) + 24
  }

  def defaultTargets(attacker: FriendlyUnitInfo, filters: TargetFilter*): (Boolean, Seq[UnitInfo]) = {
    val assigned      = attacker.targetsAssigned.getOrElse(Seq.empty)
    val matchups      = attacker.matchups.targets
    val engaged       = attacker.team.exists(_.engagedUpon)
    val targetInRange = attacker.matchups.targetsInRange.exists(hasCombatPriority)
    val allFilters    = filtersRequired(attacker) ++ filters
    val strataUnfilteredInRange = ?(
      targetInRange,
      Seq(
        (true, assigned.view.filter(hasCombatPriority).filter(attacker.inRangeToAttack).filter(acceleratesDemise(attacker, _))),
        (true, matchups.view.filter(hasCombatPriority).filter(attacker.inRangeToAttack).filter(acceleratesDemise(attacker, _))),
        (true, assigned.view.filter(hasCombatPriority).filter(attacker.inRangeToAttack)),
        (true, matchups.view.filter(hasCombatPriority).filter(attacker.inRangeToAttack))),
      Seq.empty)
    val strataUnfiltered = strataUnfilteredInRange ++ Seq(
      (true, ?(engaged, assigned.view.filter(hasCombatPriority).filter(acceleratesDemise(attacker, _)), Seq.empty)),
      (true, ?(engaged, assigned.view.filter(hasCombatPriority),                                        Seq.empty)),
      (false, assigned),
      (true, ?(engaged, matchups.view.filter(hasCombatPriority).filter(acceleratesDemise(attacker, _)), Seq.empty)),
      (true, ?(engaged, matchups.view.filter(hasCombatPriority),                                        Seq.empty)),
      (true, matchups))

    // Returns (rank by score?, targets)
    strataUnfiltered.view
      .map(p => (p._1, legalWithFilters(attacker, p._2, allFilters))).find(_._2.nonEmpty)
      .getOrElse((false, Seq.empty))
  }

  def best(attacker: FriendlyUnitInfo, filters: TargetFilter*): Option[UnitInfo] = {
    val assigned      = attacker.targetsAssigned.getOrElse(Seq.empty)
    val matchups      = attacker.matchups.targets
    val engaged       = attacker.team.exists(_.engagedUpon)
    val allFilters    = (filtersRequired(attacker) ++ filters).toVector
    val targetInRange = attacker.matchups.targetsInRange.exists(hasCombatPriority)
    var output: Option[UnitInfo] = None
    if (targetInRange) {
      // Suicide mission: inside the enemy base workers die first, combat
      // units second. In range, assigned before matchups, like the snipe
      // tiers below.
      if (attacker.isSuicideMission) {
        val suicidePick = firstInSources(attacker, assigned, matchups, allFilters, IsWorker, rankByScore = true, requireCombatPriority = false, requireInRange = true, requireAcceleratesDemise = false)
        if (suicidePick.isDefined) return suicidePick
      }

      output = focusedTarget(attacker, allFilters)
      if (output.isDefined) return output

      // Repairer snipe: a healthy unit tasked against a Terran building
      // kills the SCV currently repairing it first (unless the building
      // is nearly dead - then it finishes the building). Enforced even a
      // step out of normal range. The Focus filter is bypassed for this
      // pick on purpose; everything else still applies (no dueling
      // inside tank range).
      output = repairerSnipe(attacker, allFilters)
      if (output.isDefined) return output

      // Snipe priority: a visible Defiler, then Lurker, then Mutalisk in our
      // range outranks the generic duel pick, most wounded first inside each
      // tier. Assigned before matchups, and guaranteed-kill targets wait for
      // the reprise pass, like every other stratum on this ladder.
      def snipable(tier: Int)(target: UnitInfo): Boolean =
        ThreatPriority.snipable(target.visible, ThreatPriority.tier(Zerg.Defiler(target), Zerg.Lurker(target), Zerg.Mutalisk(target)), tier)
      // Guaranteed-kill targets wait for the reprise pass: demise-true
      // tiers first, then the same tiers again without the demise gate.
      val snipeTiers = Seq(ThreatPriority.defilerTier, ThreatPriority.lurkerTier, ThreatPriority.mutaliskTier)
      for (demise <- Seq(true, false); tier <- snipeTiers) {
        val sniped = firstInSources(attacker, assigned, matchups, allFilters, snipable(tier), rankByScore = true, requireCombatPriority = false, requireInRange = true, requireAcceleratesDemise = demise)
        if (sniped.isDefined) return sniped
      }

      output = bestInStratum(attacker, assigned, allFilters, rankByScore = true,  requireCombatPriority = true,  requireInRange = true,  requireAcceleratesDemise = true)
      if (output.isDefined) return output

      output = bestInStratum(attacker, matchups, allFilters, rankByScore = true,  requireCombatPriority = true,  requireInRange = true,  requireAcceleratesDemise = true)
      if (output.isDefined) return output

      output = bestInStratum(attacker, assigned, allFilters, rankByScore = true,  requireCombatPriority = true,  requireInRange = true,  requireAcceleratesDemise = false)
      if (output.isDefined) return output

      output = bestInStratum(attacker, matchups, allFilters, rankByScore = true,  requireCombatPriority = true,  requireInRange = true,  requireAcceleratesDemise = false)
      if (output.isDefined) return output
    }

    if (Protoss.DarkTemplar(attacker)) {
      // Dark Templar doctrine: whatever is hittable goes first, then sieged
      // tanks inside 6 tiles, then unsieged ones. Ranked by score inside each
      // stratum, assigned before matchups, like the rest of the ladder.
      def hittable(target: UnitInfo): Boolean =
        DarkTemplarTanks.hittable(attacker.tileDistanceEdge(target))
      def siegedNear(target: UnitInfo): Boolean =
        Terran.SiegeTankSieged(target) && DarkTemplarTanks.siegedNear(attacker.tileDistanceEdge(target))
      def unsiegedNear(target: UnitInfo): Boolean =
        Terran.SiegeTankUnsieged(target) && DarkTemplarTanks.unsiegedNear(attacker.tileDistanceEdge(target))

      for (doctrine <- Seq[UnitInfo => Boolean](hittable, siegedNear, unsiegedNear)) {
        val darkPick = firstInSources(attacker, assigned, matchups, allFilters, doctrine, rankByScore = true, requireCombatPriority = false, requireInRange = false, requireAcceleratesDemise = false)
        if (darkPick.isDefined) return darkPick
      }
    }

    if (Protoss.Carrier(attacker) && CarrierTankHuntSeam.huntFor(attacker)) {
      // Carrier doctrine: a hunting wing always goes for the tanks in
      // range, sieged first. Ranked by score inside each stratum, assigned
      // before matchups, like the rest of the ladder.
      def siegedNear(target: UnitInfo): Boolean =
        Terran.SiegeTankSieged(target) && attacker.tileDistanceEdge(target) <= CarrierTankHunt.tankHuntTiles
      def unsiegedNear(target: UnitInfo): Boolean =
        Terran.SiegeTankUnsieged(target) && attacker.tileDistanceEdge(target) <= CarrierTankHunt.tankHuntTiles

      for (doctrine <- Seq[UnitInfo => Boolean](siegedNear, unsiegedNear)) {
        val carrierPick = firstInSources(attacker, assigned, matchups, allFilters, doctrine, rankByScore = true, requireCombatPriority = false, requireInRange = false, requireAcceleratesDemise = false)
        if (carrierPick.isDefined) return carrierPick
      }
    }

    if (engaged) {
      output = bestInStratum(attacker, assigned, allFilters, rankByScore = true,  requireCombatPriority = true,  requireInRange = false, requireAcceleratesDemise = true)
      if (output.isDefined) return output

      output = bestInStratum(attacker, assigned, allFilters, rankByScore = true,  requireCombatPriority = true,  requireInRange = false, requireAcceleratesDemise = false)
      if (output.isDefined) return output
    }

    output = bestInStratum(attacker, assigned, allFilters, rankByScore = false, requireCombatPriority = false, requireInRange = false, requireAcceleratesDemise = false)
    if (output.isDefined) return output

    if (engaged) {
      output = bestInStratum(attacker, matchups, allFilters, rankByScore = true, requireCombatPriority = true, requireInRange = false, requireAcceleratesDemise = true)
      if (output.isDefined) return output

      output = bestInStratum(attacker, matchups, allFilters, rankByScore = true, requireCombatPriority = true, requireInRange = false, requireAcceleratesDemise = false)
      if (output.isDefined) return output
    }

    bestInStratum(attacker, matchups, allFilters, rankByScore = true, requireCombatPriority = false, requireInRange = false, requireAcceleratesDemise = false)
  }

  // Assigned-before-matchups sweep for one predicate: the ladder's atom.
  // Every doctrine above walks the same two sources in the same order;
  // only the predicate and the flags change.
  private def firstInSources(
    attacker: FriendlyUnitInfo,
    assigned: Iterable[UnitInfo],
    matchups: Iterable[UnitInfo],
    allFilters: IndexedSeq[TargetFilter],
    predicate: UnitInfo => Boolean,
    rankByScore: Boolean,
    requireCombatPriority: Boolean,
    requireInRange: Boolean,
    requireAcceleratesDemise: Boolean): Option[UnitInfo] = {
    val inAssigned = bestInStratum(attacker, assigned.view.filter(predicate), allFilters, rankByScore, requireCombatPriority, requireInRange, requireAcceleratesDemise)
    if (inAssigned.isDefined) return inAssigned

    bestInStratum(attacker, matchups.view.filter(predicate), allFilters, rankByScore, requireCombatPriority, requireInRange, requireAcceleratesDemise)
  }

  private def bestInStratum(
    attacker: FriendlyUnitInfo,
    targets: Iterable[UnitInfo],
    allFilters: IndexedSeq[TargetFilter],
    rankByScore: Boolean,
    requireCombatPriority: Boolean,
    requireInRange: Boolean,
    requireAcceleratesDemise: Boolean): Option[UnitInfo] = {
    var best        : UnitInfo = null
    var bestScore   : Double   = 0.0
    var found       : Boolean  = false
    val targetIterator = targets.iterator
    while (targetIterator.hasNext) {
      val target = targetIterator.next()
      val legal = ( ! requireCombatPriority || hasCombatPriority(target)) && ( ! requireInRange || attacker.inRangeToAttack(target)) && ( ! requireAcceleratesDemise || acceleratesDemise(attacker, target)) && legalByFilters(attacker, target, allFilters)
      if (legal) {
        if ( ! rankByScore) return Some(target)

        val score = attacker.targetScore(target)
        if ( ! found || score > bestScore) {
          found = true
          best = target
          bestScore = score
        }
      }
    }
    ?(found, Some(best), None)
  }

  // Mass fights fire as one: the squad's weakest reachable target outranks
  // each unit's own nearest pick, so damage concentrates instead of
  // spreading over the enemy line. Shooters that cannot reach it yet keep the
  // normal ladder (and the formation march) instead of chasing across the
  // line. Outside a mass fight this is None and the ladder decides.
  private def focusedTarget(attacker: FriendlyUnitInfo, allFilters: IndexedSeq[TargetFilter]): Option[UnitInfo] =
    FocusFire
      .sharedTarget(attacker)
      .filter(target => attacker.inRangeToAttack(target) && legalByFilters(attacker, target, allFilters))

  private def repairerSnipe(attacker: FriendlyUnitInfo, allFilters: IndexedSeq[TargetFilter]): Option[UnitInfo] = {
    if (attacker.totalHealth < RepairerSnipe.healthyHp) return None

    val buildings = (attacker.agent.toAttack.toSeq ++ attacker.targetsAssigned.getOrElse(Seq.empty))
      .filter(b => b.unitClass.isBuilding && b.player.isTerran && b.alive && RepairerSnipe.shouldSnipe(attacker.totalHealth, b.totalHealth))
    if (buildings.isEmpty) return None

    val repairing = buildings.toSet
    Maff.minBy(attacker.matchups.targets.view.filter(t =>
      Terran.SCV(t)
      && t.visible
      && t.order == Orders.Repair
      && t.orderTarget.exists(repairing.contains)
      && attacker.canAttack(t)
      && RepairerSnipe.inChaseRange(
        attacker.tileDistanceEdge(t),
        attacker.pixelRangeAgainst(t) * Maff.inv32)))(
      attacker.pixelDistanceEdge)
      .filter(t => legalByFilters(attacker, t, allFilters.filterNot(_ == TargetFilterFocus)))
  }

  // Confirmed corpses are never legal: remembered units keep alive=true,
  // so this only drops Visibility.Dead, in every stratum at once.
  private def legalByFilters(attacker: FriendlyUnitInfo, target: UnitInfo, allFilters: IndexedSeq[TargetFilter]): Boolean = {
    if ( ! target.alive) return false

    if (With.yolo.active) return attacker.canAttack(target) && TargetFilterType.legal(attacker, target)

    var filterIndex = 0
    while (filterIndex < allFilters.length) {
      if ( ! allFilters(filterIndex).legal(attacker, target)) return false

      filterIndex += 1
    }
    true
  }

  def best(attacker: FriendlyUnitInfo, whitelist: Iterable[UnitInfo]): Option[UnitInfo] = {
    best(attacker, TargetFilterWhitelist(whitelist))
  }

  /**
   * Memory release (pure booleans for the suite): a fogged contact never
   * outranks a live one. With another hittable target in range the ladder
   * must re-pick instead of posing at a memory - the caller feeds live
   * visibility; this only combines the flags.
   */
  def shouldDropMemory(targetVisible: Boolean, otherHittableInRange: Boolean): Boolean =
    ! targetVisible && otherHittableInRange

  def bestUnfiltered(attacker: FriendlyUnitInfo, targets: Iterable[UnitInfo]): Option[UnitInfo] = {
    Maff.maxBy(targets.filter(_.alive))(attacker.targetScore)
  }

  def legal(attacker: FriendlyUnitInfo, targets: Seq[UnitInfo], filters: TargetFilter*): Seq[UnitInfo] = {
    legalWithFilters(attacker, targets, filtersRequired(attacker) ++ filters)
  }

  private def legalWithFilters(attacker: FriendlyUnitInfo, targets: Seq[UnitInfo], allFilters: Seq[TargetFilter]): Seq[UnitInfo] = {
    targets.view.filter(target => target.alive && ?(With.yolo.active,
      attacker.canAttack(target) && TargetFilterType.legal(attacker, target),
      allFilters.forall(_.legal(attacker, target))))
  }

  def filtersRequired(attacker: FriendlyUnitInfo): Seq[TargetFilter] = {
    defaultFilters.view.filter(_.appliesTo(attacker))
  }

  def auditLegality(attacker: FriendlyUnitInfo, additionalFiltersRequired: TargetFilter*): Vector[(UnitInfo, Vector[(Boolean, TargetFilter)])] = {
    defaultTargets(attacker)._2
      .map(target => (
        target,
        (filtersRequired(attacker) ++ additionalFiltersRequired.filter(_.appliesTo(attacker)))
          .map(filter => (filter.legal(attacker, target), filter))
          .toVector
          .sortBy(_._1)
      ))
      .toVector
  }

  def auditScore(attacker: FriendlyUnitInfo): Vector[(UnitInfo, String, String, String)] = {
    defaultTargets(attacker)._2
      .map(target => (
        target,
        attacker.pixelDistanceEdge(target) * Maff.inv32,
        target.targetValue,
        attacker.targetScore(target)))
      .toVector
      .sortBy(-_._4)
      .map(s =>
        (s._1,
          s._2.toInt.toString,
          s._3.toInt.toString,
          s._4.toInt.toString))
  }
}


