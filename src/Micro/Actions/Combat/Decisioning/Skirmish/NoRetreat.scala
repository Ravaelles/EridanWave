package Micro.Actions.Combat.Decisioning.Skirmish

import Information.Battles.ArmyStrength
import Information.Geography.Chokes
import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Protoss.Dragoon.DragoonBrave
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import Utilities.UnitFilters.IsTank

/**
 * Single registry of critical no-retreat situations (SRP: one reason to
 * change - the list of fights we never walk away from; OCP: a new
 * situation adds a method, existing ones stay untouched). Each method
 * reads the world for one situation and delegates the numbers to its
 * doctrine kernel, which keeps its own suite. FightRules only dispatches.
 */
object NoRetreat {

  /** In the enemy main the mission owns the unit, never the sim. */
  def suicideMission(unit: FriendlyUnitInfo): Boolean =
    unit.isSuicideMission

  /** Early shielded Dragoons trade Marines instead of kiting them. */
  def dragoonBrave(unit: FriendlyUnitInfo): Boolean =
    DragoonBrave.applies(
      Protoss.Dragoon(unit) && With.enemies.exists(_.isTerran),
      unit.shieldPoints,
      unit.cooldownLeft,
      With.self.supplyUsed200,
      ArmyStrength.score(With.battles.globalAttack.us.skimStrengthTotal, With.battles.globalAttack.enemy.skimStrengthTotal),
      unit.matchups.threats.nonEmpty && unit.matchups.threats.forall(Terran.Marine))

  /** Between our main and the natural choke minus four warriors hold home. */
  def homeStand(unit: FriendlyUnitInfo): Boolean =
    unit.unitClass.isWarrior && With.geography.ourBases.nonEmpty && Chokes.naturalChoke.exists(choke => {
      val mainHeart = With.geography.ourMain.heart.center
      HomeStand.holdsGround(
        unit.pixel.groundPixels(mainHeart) * Maff.inv32,
        HomeStand.chokeDistToMain(mainHeart, choke.pixelCenter))
    })

  /** Siege tanks this close to our natural, in tiles, must die standing. */
  val naturalTankTiles: Double = 18.0

  def threatensNatural(distTiles: Double): Boolean =
    distTiles <= naturalTankTiles

  /** Siege tanks on our natural are attacked, never retreated from. */
  def tanksAtNatural(unit: FriendlyUnitInfo): Boolean =
    unit.unitClass.isWarrior && With.geography.ourBases.nonEmpty && {
      val naturalHeart = With.geography.ourNatural.heart.center
      With.units.enemy.exists(t =>
        t.likelyStillThere && IsTank(t) &&
          threatensNatural(t.tileDistanceEdge(naturalHeart)))
    }
}
