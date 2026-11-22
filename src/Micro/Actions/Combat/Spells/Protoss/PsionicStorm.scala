package Micro.Actions.Combat.Spells.Protoss

import Micro.Actions.UnitControl
import Lifecycle.With
import Mathematics.Points.Pixel
import Micro.Actions.Combat.Spells.{SpellValue, TargetedSpell}
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

  /** Storms lead moving victims this far along their heading, in tiles; standing ones keep the spot. */
  val stormLeadTiles: Double = 2.0

  override protected def leadTiles: Double = stormLeadTiles

  /**
   * Casters walk with the attack: a personal retreat verdict strands a
   * full battery behind the army while Hydras chew it 300 pixels away,
   * out of the threat band but inside storm range. The squad consensus
   * re-arms the walk; the leash, the pack ladder and the friendly veto
   * still gate the cast itself.
   */
  override protected def additionalConditions(unit: FriendlyUnitInfo): Boolean =
    unit.agent.shouldFight ||
      unit.matchups.threatsInRange.nonEmpty ||
      unit.base.exists(_.owner.isEnemy) ||
      unit.squad.exists(_.fightConsensus)

  /**
   * Storm hunt radius, in tiles: just inside the nine-tile cast range.
   * The old leash reached three tiles past it, so a Templar picked a pack
   * it could not hit, committed the intent, and walked into Hydralisk
   * range to land the cast - five Templars died that way with a full
   * battery. A storm is a standoff weapon: only pick victims already in
   * reach, and the small box-centre step is all the walking left.
   */
  val stormSearchTiles: Double = 8.9

  override protected def searchTiles: Option[Double] =
    Some(stormSearchTiles)

  val castFrames = 4 // Derived from iScript; observed empirically by McRave

  /** Own units that may share a storm, at most: the third is the veto. */
  val maxFriendlyHits: Int = 2

  /**
   * Friendly-fire veto (pure count for the suite): a storm box catching
   * more than two of ours never casts, however juicy the trade looks.
   */
  def vetoFriendlyHits(friendlyHits: Int): Boolean =
    friendlyHits > maxFriendlyHits

  /** Overlords eat storms only on a near-full battery: cheap, numerous, and
    * rarely worth energy that could erase a Hydra pack instead. Deliberately
    * above the pack ladder below: a lone Overlord never outranks a lone
    * Hydralisk for the same storm. */
  val overlordStormEnergy: Int = 190

  def vetoOverlord(isOverlord: Boolean, energy: Int): Boolean =
    isOverlord && energy < overlordStormEnergy

  /** Shared-pack minimum vs Protoss. */
  val packBaseVsProtoss: Int = 3

  /** Shared-pack minimum vs others. */
  val packBaseVsOthers: Int = 4

  /** At or above this energy a lone victim is worth a storm. */
  val packSingleEnergy: Int = 180

  /** At or above this energy a pair is worth a storm. */
  val packPairEnergy: Int = 100

  /** Below this energy hold fire entirely: not even a full pack is worth the last storm. */
  val packFloorEnergy: Int = 75

  /**
   * Energy-scaled pack minimum (pure numbers for the suite): a full
   * battery storms anything, a half battery storms pairs, a low one
   * only storms a pack - and below 75 nothing casts at all.
   */
  def minPackEnemies(energy: Int, enemyIsProtoss: Boolean): Int = {
    if (energy >= packSingleEnergy) return 1
    if (energy >= packPairEnergy) return 2
    if (energy >= packFloorEnergy) return ?(enemyIsProtoss, packBaseVsProtoss, packBaseVsOthers)

    Int.MaxValue
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
    // stasised/invincible ride SpellTargets.legal, which filters every
    // candidate before valueTarget runs - no second copy here.
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

    val multiplierConfidence  = SpellValue.confidence(caster.confidence11)
    val multiplierPlayer      = if (target.isEnemy) 1.0 else if (target.isFriendly) -2.0 else 0.0
    val multiplierUnit        = target.unitClass.stormValue
    val multiplierDanger      = SpellValue.danger(caster.matchups.threatsInRange.nonEmpty, caster.unitClass.maxTotalHealth.toDouble, caster.totalHealth)
    val multiplierRich        = Math.max(1.0, 0.25 * caster.team.map(_.stormCount).getOrElse(1))
    val output                = SpellValue.of(multiplierUnit, multiplierPlayer, multiplierConfidence, multiplierDanger, multiplierRich)
    output
  }
  
  override def onCast(caster: FriendlyUnitInfo, target: Pixel): Unit = {
    With.grids.psionicStorm.addPsionicStorm(target)
  }

  override protected def perform(unit: FriendlyUnitInfo): UnitControl = {
    if (SuicidePsionic.act(unit)) return UnitControl.Claimed

    super.perform(unit)
  }
}
