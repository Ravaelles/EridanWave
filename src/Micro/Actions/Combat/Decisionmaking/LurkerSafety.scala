package Micro.Actions.Combat.Decisionmaking

import Lifecycle.With
import Macro.Actions.MacroActions
import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Zerg}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.Time.Seconds

/**
 * Lurker safety, ported from Atlantis AvoidLurkers: ground units avoid
 * burrowed Lurkers they cannot fight and fight them when they can.
 *
 * Per-unit-type exceptions use the Strategy pattern below: policies are
 * pure functions of facts (no game state inside), so each kind reads as
 * one rule set and stays unit-testable. Zealots flee burrowing Lurkers
 * unconditionally, ranged units defer to detectors and army.
 */
object LurkerSafety extends MacroActions {

  /** Lurker radius that matters, in tiles (wound-scaled per policy). */
  var baseRadiusTiles: Double = 8.1

  /** Wounded units keep extra distance, linearly, up to this (tiles at full wounds). */
  var woundBonusTiles: Double = 2.5

  /** Melee units keep this extra distance (they cannot trade spines). */
  var meleeBonusTiles: Double = 2.5

  /** Workers keep this extra distance. */
  var workerBonusTiles: Double = 1.5

  /** Without our own mobile detectors the radius grows by this (tiles). */
  var noDetectorBonusTiles: Double = 1.8

  def avoidanceRadiusTiles(unit: FriendlyUnitInfo): Double =
    baseRadiusTiles + LurkerPolicy.of(unit.unitClass.isWorker, unit.unitClass.melee)
      .radiusBonusTiles(unit.woundPercent())

  sealed trait LurkerPolicy {
    def radiusBonusTiles(wound01: Double): Double
    def fleesBurrowing(detectorsNear: Boolean): Boolean
    def braveAgainstBurrowed(detectorsNear: Boolean, cannonsCover: Boolean, bigArmy: Boolean): Boolean
  }

  /** Zealots die standing next to a burrowing Lurker during its transition. */
  case object ZealotPolicy extends LurkerPolicy {
    def radiusBonusTiles(wound01: Double): Double =
      meleeBonusTiles + woundBonusTiles * Maff.clamp(wound01, 0.0, 1.0)

    def fleesBurrowing(detectorsNear: Boolean): Boolean =
      ! detectorsNear

    def braveAgainstBurrowed(detectorsNear: Boolean, cannonsCover: Boolean, bigArmy: Boolean): Boolean =
      detectorsNear || cannonsCover
  }

  /** Ranged units trade fine once they can see and outrange the spines. */
  case object RangedPolicy extends LurkerPolicy {
    def radiusBonusTiles(wound01: Double): Double =
      woundBonusTiles * Maff.clamp(wound01, 0.0, 1.0)

    def fleesBurrowing(detectorsNear: Boolean): Boolean =
      false

    def braveAgainstBurrowed(detectorsNear: Boolean, cannonsCover: Boolean, bigArmy: Boolean): Boolean =
      detectorsNear || cannonsCover || bigArmy
  }

  /** Workers run from everything; they only widen the radius. */
  case object WorkerPolicy extends LurkerPolicy {
    def radiusBonusTiles(wound01: Double): Double =
      workerBonusTiles + woundBonusTiles * Maff.clamp(wound01, 0.0, 1.0)

    def fleesBurrowing(detectorsNear: Boolean): Boolean =
      ! detectorsNear

    def braveAgainstBurrowed(detectorsNear: Boolean, cannonsCover: Boolean, bigArmy: Boolean): Boolean =
      false
  }

  object LurkerPolicy {
    def of(isWorker: Boolean, melee: Boolean): LurkerPolicy = {
      if (isWorker) WorkerPolicy
      else if (melee) ZealotPolicy
      else RangedPolicy
    }
  }

  def detectorsNear(unit: FriendlyUnitInfo, tiles: Double = 7.5): Boolean =
    With.units.ours.exists(u =>
      u.unitClass.isDetector && ! u.unitClass.isBuilding
      && u.pixelDistanceCenter(unit.pixel) < tiles * 32)

  def cannonsCover(unit: FriendlyUnitInfo, lurker: UnitInfo, tiles: Double = 6.4): Boolean =
    With.units.ours.exists(u =>
      Protoss.PhotonCannon(u) && u.pixelDistanceCenter(lurker.pixel) < tiles * 32)

  def bigArmyWithObservers(unit: FriendlyUnitInfo): Boolean =
    units(Protoss.Observer) > 1 &&
    unit.shieldPoints > 5 &&
    With.units.ours.count(u => u.pixelDistanceCenter(unit.pixel) < 13 * 32) >= 14

  def lurkersToAvoid(unit: FriendlyUnitInfo, radiusTiles: Double): Seq[UnitInfo] =
    With.units.enemy.filter(e =>
      Zerg.Lurker(e) && e.likelyStillThere && e.burrowed
      && e.pixelDistanceCenter(unit.pixel) < radiusTiles * 32).toSeq

  def burrowingNear(unit: FriendlyUnitInfo, radiusTiles: Double): Seq[UnitInfo] =
    With.units.enemy.filter(e =>
      Zerg.LurkerEgg(e) && e.likelyStillThere
      && e.pixelDistanceCenter(unit.pixel) < radiusTiles * 32).toSeq

  def shouldAvoid(unit: FriendlyUnitInfo): Boolean = {
    if (unit.flying || unit.unitClass.isBuilding) {
      false
    } else {
      val policy = LurkerPolicy.of(unit.unitClass.isWorker, unit.unitClass.melee)
      val radius = avoidanceRadiusTiles(unit)
      val detectors = detectorsNear(unit)
      val fleesEggs = policy.fleesBurrowing(detectors) && burrowingNear(unit, radius).nonEmpty
      val fearsBurrowed = lurkersToAvoid(unit, radius).exists(lurker =>
        ! policy.braveAgainstBurrowed(detectors, cannonsCover(unit, lurker), bigArmyWithObservers(unit)))

      fleesEggs || fearsBurrowed
    }
  }

  def recentlyDamaged(unit: FriendlyUnitInfo): Boolean =
    With.framesSince(unit.lastFrameTakingDamage) < Seconds(6)()
}
