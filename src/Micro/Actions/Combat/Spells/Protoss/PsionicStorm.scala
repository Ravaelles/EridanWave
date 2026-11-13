package Micro.Actions.Combat.Spells.Protoss

import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.Pixel
import Micro.Actions.Combat.Spells.TargetedSpell
import ProxyBwapi.Races.{Protoss, Terran, Zerg}
import ProxyBwapi.Techs.Tech
import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.?

object PsionicStorm extends TargetedSpell {
  
  override protected def casterClass      : UnitClass = Protoss.HighTemplar
  override protected def tech             : Tech      = Protoss.PsionicStorm
  override protected def aoe              : Boolean   = true
  override protected def castRangeTiles   : Int       = 9
  override protected def thresholdValue   : Double    = 1.0
  override protected def lookaheadPixels  : Int       = 12
  override protected def additionalConditions(unit: FriendlyUnitInfo): Boolean = unit.agent.shouldFight || unit.matchups.threatsInRange.nonEmpty || unit.base.exists(_.owner.isEnemy)

  val castFrames = 4 // Derived from iScript; observed empirically by McRave

  /** Own units that may share a storm, at most: the third is the veto. */
  val maxFriendlyHits: Int = 2

  /**
   * Friendly-fire veto (pure count for the suite): a storm box catching
   * more than two of ours never casts, however juicy the trade looks.
   */
  def vetoFriendlyHits(friendlyHits: Int): Boolean =
    friendlyHits > maxFriendlyHits

  /** Overlords eat storms only on a full battery: cheap, numerous, and
    * rarely worth 75 energy that could erase a Hydra pack instead.
    * Aligned with the pack ladder below (Atlantis storms singles at 195). */
  val overlordStormEnergy: Int = 195

  def vetoOverlord(isOverlord: Boolean, energy: Int): Boolean =
    isOverlord && energy < overlordStormEnergy

  /** Shared-pack minimum vs Protoss (Atlantis: three, two past 110). */
  val packBaseVsProtoss: Int = 3

  /** Shared-pack minimum vs others (Atlantis: four, three past 110). */
  val packBaseVsOthers: Int = 4

  /** At or above this energy a lone victim is worth a storm. */
  val packSingleEnergy: Int = 195

  /** At or above this energy a pair is worth a storm. */
  val packPairEnergy: Int = 180

  /** At or above this energy the minimum drops by one. */
  val packDiscountEnergy: Int = 110

  /**
   * Energy-scaled pack minimum (pure numbers for the suite, Atlantis
   * ladder): a full battery storms anything, a dry one only storms
   * a pack - no 75-energy answers to a lone Marine.
   */
  def minPackEnemies(energy: Int, enemyIsProtoss: Boolean): Int = {
    if (energy >= packSingleEnergy) return 1
    if (energy >= packPairEnergy) return 2

    val base = ?(enemyIsProtoss, packBaseVsProtoss, packBaseVsOthers)
    ?(energy >= packDiscountEnergy, base - 1, base)
  }

  /**
   * Crucial targets bypass the count: a Reaver or a sieged Tank dies to
   * one storm, so the pack rule never protects them (Atlantis storms
   * these two at any battery above the cast cost).
   */
  def crucialStormTarget(target: UnitInfo): Boolean =
    target.isEnemy && target.isAny(Protoss.Reaver, Terran.SiegeTankSieged)

  /**
   * Thin-pack veto (pure flags for the suite): below the energy-scaled
   * minimum a box without a crucial target never casts.
   */
  def vetoThinPack(foeCount: Int, hasCrucialTarget: Boolean, energy: Int, enemyIsProtoss: Boolean): Boolean =
    ! hasCrucialTarget && foeCount < minPackEnemies(energy, enemyIsProtoss)

  override protected def vetoBox(units: Iterable[UnitInfo], caster: FriendlyUnitInfo): Boolean =
    vetoFriendlyHits(units.count(_.isFriendly)) || vetoThinPack(
      foeCount          = units.count(_.isEnemy),
      hasCrucialTarget  = units.exists(crucialStormTarget),
      energy            = caster.energy,
      enemyIsProtoss    = With.enemies.exists(_.isProtoss))

  override protected def valueTarget(target: UnitInfo, caster: FriendlyUnitInfo): Double = {
    if (With.grids.psionicStorm.isSet(target.tile)) return 0.0
    if (vetoOverlord(Zerg.Overlord(target), caster.energy)) return 0.0
    if ( ! target.unitClass.canBeStormed) return 0.0
    if (target.underStorm)  return 0.0
    if (target.stasised)    return 0.0
    if (target.invincible)  return 0.0
    if ( ! target.visible)  return 0.0
    if (target.isFriendly && target.isTransport && target.loadedUnitCount == 0) return 0.0 // Dumb hack to ensure our storm drops don't try too hard to avoid the shuttle
    if (target.isAny(Protoss.Interceptor, Zerg.Larva, Zerg.Egg, Zerg.LurkerEgg)) return 0.0

    lazy val distance       = target.pixelDistanceCenter(caster)
    lazy val travelDistance = Math.max(0, distance - castRangePixels)
    lazy val exposedFrames  = With.latency.latencyFrames + castFrames + travelDistance / caster.unitClass.topSpeed
    lazy val castTile       = target.pixel.project(caster.pixel, Math.min(castRangePixels, distance)).tile
    if (target.player.isTerran
      && (caster.transport.isDefined || ! caster.matchups.inTankRange)
      && travelDistance > 0
      && castTile.enemiesAttackingGround.exists(t => Terran.SiegeTankSieged(t) && t.cooldownLeft < exposedFrames && ! t.inRangeToAttack(caster))) {
      return 0.0
    }

    val multiplierConfidence  = Math.max(1.0, Maff.nanToOne(1 / (1 + caster.confidence11)))
    val multiplierPlayer      = if (target.isEnemy) 1.0 else if (target.isFriendly) -2.0 else 0.0
    val multiplierUnit        = target.unitClass.stormValue
    val multiplierDanger      = 1.0 + 0.5 * (if (caster.matchups.threatsInRange.nonEmpty) caster.unitClass.maxTotalHealth.toDouble / caster.totalHealth else 0.0)
    val multiplierRich        = Math.max(1.0, 0.25 * caster.team.map(_.stormCount).getOrElse(1))
    val output                = multiplierConfidence * multiplierPlayer * multiplierUnit * multiplierDanger * multiplierRich
    output
  }
  
  override def onCast(caster: FriendlyUnitInfo, target: Pixel): Unit = {
    With.grids.psionicStorm.addPsionicStorm(target)
  }
}
