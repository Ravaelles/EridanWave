package Tactic.Squads.Decisions

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Tactic.Squads.Group.FriendlyUnitGroup
import Utilities.Latch

/**
 * Single suicide-mission detector (renamed from SuicideChargeSeam): a ground
 * warrior inside the enemy base never retreats, holds formation or dresses
 * the line - it only attacks, workers first. Thin on purpose: the distances
 * stay in the pure tested kernel (SuicideAssault). assign commits the unit,
 * isActive reads live with no stored state to go stale - and held latches
 * the verdict for every gate, so a hull jittering on the 15-tile line stays
 * committed instead of flapping the march and the duel every other frame
 * (and splitting the army in half).
 */
object SuicideMission {

  private def mainHeart = With.scouting.enemyMain.map(_.heart.center)

  private def enemyIsZerg: Boolean =
    With.enemies.exists(_.isZerg)

  /**
   * Commit the unit to the mission while it holds; true while it holds.
   */
  def assign(unit: FriendlyUnitInfo): Boolean = {
    val active = isActive(unit)

    if (active) unit.agent.commit = true

    active
  }

  /** Live read, no stored state: true while the unit sits in the enemy base. */
  def isActive(unit: FriendlyUnitInfo): Boolean =
    inZone(unit) || inMainAssault(unit)

  /**
   * Latched mission (one verdict for every gate): entry is the sharp
   * detector above, release only past both bands. Every movement-owning
   * gate reads this, never the raw edge - the release absorbs the
   * mill jitter the detector cannot see.
   */
  private val mission = new Latch

  def held(unit: FriendlyUnitInfo): Boolean =
    mission(unit.id, fires = isActive(unit), releases = released(unit))

  private def released(unit: FriendlyUnitInfo): Boolean =
    unit.flying || ! unit.unitClass.isWarrior || mainHeart.forall(heart =>
      SuicideAssault.released(
        unit.tileDistanceCenter(heart),
        unit.pixel.groundPixels(heart) * Maff.inv32))

  def squadInZone(squad: FriendlyUnitGroup): Boolean =
    enemyIsZerg && mainHeart.exists(heart =>
      SuicideAssault.charging(squad.centroidGround.groundPixels(heart) * Maff.inv32))

  private def inZone(unit: FriendlyUnitInfo): Boolean =
    ! unit.flying && unit.unitClass.isWarrior && enemyIsZerg && mainHeart.exists(heart =>
      SuicideAssault.charging(unit.pixel.groundPixels(heart) * Maff.inv32))

  /**
   * Any race, straight tile distance: a ground warrior inside the enemy
   * main commits to the assault instead of posing or retreating. Euclidean
   * on purpose - ground-path distance goes blind where UMS ground does not
   * connect, which is exactly where packs mill around in formation.
   */
  private def inMainAssault(unit: FriendlyUnitInfo): Boolean =
    ! unit.flying && unit.unitClass.isWarrior && mainHeart.exists(heart =>
      SuicideAssault.assaulting(unit.tileDistanceCenter(heart)))
}
