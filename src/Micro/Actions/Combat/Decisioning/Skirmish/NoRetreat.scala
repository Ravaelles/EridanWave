package Micro.Actions.Combat.Decisioning.Skirmish

import Information.Battles.ArmyStrength
import Information.Geography.Chokes
import Lifecycle.Enemy
import Lifecycle.With
import Mathematics.Maff
import Micro.Actions.Protoss.Dragoon.DragoonBrave
import Micro.Actions.Protoss.Dragoon.DragoonPackStand
import Micro.Actions.Protoss.ReaverShuttle.ReaverBrave
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Tactic.Squads.Decisions.SuicideMission
import Utilities.Latch
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
    SuicideMission.held(unit)

  /** Early shielded Dragoons trade Marines instead of kiting them. */
  def dragoonBrave(unit: FriendlyUnitInfo): Boolean =
    DragoonBrave.applies(
      Protoss.Dragoon(unit) && Enemy.terran(),
      unit.shieldPoints,
      unit.cooldownLeft,
      With.self.supplyUsed200,
      ArmyStrength.score(With.battles.globalAttack.us.skimStrengthTotal, With.battles.globalAttack.enemy.skimStrengthTotal),
      unit.matchups.threats.nonEmpty && unit.matchups.threats.forall(Terran.Marine))

  /**
   * A Dragoon pack against light Terran holds the line: three Dragoons
   * outtrade Vultures and infantry, so only a dying hull runs. Applies to
   * Dragoons only - a worker caught in the same contact keeps its own
   * verdict instead of standing into Marines.
   */
  def dragoonPack(unit: FriendlyUnitInfo): Boolean =
    Protoss.Dragoon(unit) && DragoonPackStand.shouldStand(
      enemyIsTerran = Enemy.terran(),
      dragoonCount = unit.matchups.allies.count(Protoss.Dragoon) + 1,
      lightOnly = unit.matchups.threats.nonEmpty && unit.matchups.threats.forall(DragoonPackStand.isLightTerran),
      totalHp = unit.totalHealth)

  /**
   * A healthy escorted Reaver is a firing platform: the sim's blanket
   * Fallback sent it walking away from packs it outranges ("Fallback/
   * Retreat" with 150 health, allies beside it and a full scarab), so a
   * braced Reaver stands and shoots what is already in reach instead.
   */
  def reaverBrave(unit: FriendlyUnitInfo): Boolean =
    ReaverBrave.applies(
      Protoss.Reaver(unit),
      unit.totalHealth,
      unit.matchups.allies.exists(a =>
        ! a.flying && a.unitClass.isWarrior && a.pixelDistanceEdge(unit) <= ReaverBrave.escortTiles * Maff.inv32),
      unit.matchups.targetsInRange.exists(t =>
        unit.pixelDistanceEdge(t) <= ReaverBrave.reachTiles * Maff.inv32),
      unit.cooldownLeft)

  /** Between our main and the natural choke minus four warriors hold home. */
  def homeStand(unit: FriendlyUnitInfo): Boolean =
    unit.unitClass.isWarrior && With.geography.ourBases.nonEmpty && Chokes.naturalChoke.exists(choke => {
      val mainHeart = With.geography.ourMain.heart.center
      HomeStand.holdsGround(
        unit.pixel.groundPixels(mainHeart) * Maff.inv32,
        HomeStand.chokeDistToMain(mainHeart, choke.pixelCenter))
    })

  /**
   * Standing ground (one predicate for the posture floor and the
   * technique lock): any sacred-ground rule firing pins the whole
   * cluster - a single hull that must not retreat vetoes a Withdraw
   * for everyone, the way one vote already decides a consensus.
   */
  def holdsAny(unit: FriendlyUnitInfo): Boolean =
    homeStand(unit) || tanksAtNatural(unit) || dragoonBrave(unit) ||
      dragoonPack(unit) || reaverBrave(unit) || proxyDefense() ||
      retreatForbidden(unit) ||
      umsEndgame(With.umsMode, With.frame, unit.matchups.targetsInRange.nonEmpty)

  /** Ten seconds in, UMS micro maps stop retreating for good. */
  val umsNoRetreatFrames: Int = Utilities.Time.Seconds(10)()

  /**
   * UMS endgame (pure flags for the suite): past ten seconds a unit
   * with nothing in range holds instead of withdrawing - endgame
   * chases die the moment a kiter baits a withdrawal, so the line
   * only advances. Duels keep the old vote: a hull trading hits may
   * still withdraw when the skim says so. Ladder games never read it.
   */
  def umsEndgame(umsMode: Boolean, frame: Int, hasTargetInRange: Boolean): Boolean =
    umsMode && frame >= umsNoRetreatFrames && ! hasTargetInRange

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

  /**
   * Forbidden retreat (owner order): an enemy warrior on our natural
   * choke line - or up to four tiles outside it, or already past it
   * toward the main - with our hull inside dueling range means the line
   * fights where it stands. Retreating through the choke donates it and
   * the game walks out the back door while the duel dances in place -
   * the f14215 Retreat/Engage flap. PvP and PvZ only; Terran sieges keep
   * their own doctrine. Latched like the dress latch: entry is sharp,
   * the release band absorbs mill noise so the verdict never flaps on
   * the boundary. A second, race-blind stand covers the natural itself:
   * with at least a handful home, any foe at our door is answered.
   */
  private val forbiddenLatch = new Latch

  def retreatForbidden(unit: FriendlyUnitInfo): Boolean = {
    // Ladder homes only, like the proxy stand above.
    if (With.umsMode) return false

    val choke = Chokes.naturalChoke.map(_.pixelCenter)
    if (choke.isEmpty || With.geography.ourBases.isEmpty) return false

    val home = With.geography.ourMain.heart.center
    val natural = With.geography.ourNatural.heart.center
    val homeRadius = Information.Geography.Distances.naturalChokeToMainBase()
    val raceOk = Enemy.protoss() || Enemy.zerg()
    val foes = With.units.enemy.view.filter(foe =>
      foe.alive && foe.likelyStillThere && foe.unitClass.isWarrior).toSeq
    val ourWarriors = With.units.ours.count(u => u.alive && u.unitClass.isWarrior)
    def foeMain(foe: UnitInfo): Double = foe.pixel.groundPixels(home) * Maff.inv32
    def foeNatural(foe: UnitInfo): Double = foe.pixel.groundPixels(natural) * Maff.inv32
    def ourDist(foe: UnitInfo): Double = unit.pixelDistanceCenter(foe.pixel) * Maff.inv32
    forbiddenLatch(unit.id,
      fires =
        (raceOk && foes.exists(foe =>
          Tactic.Squads.Decisions.RetreatForbidden.forbidden(true, foeMain(foe), homeRadius, ourDist(foe)))) ||
        foes.exists(foe =>
          Tactic.Squads.Decisions.RetreatForbidden.defendNatural(ourWarriors, foeNatural(foe), homeRadius)),
      releases =
        foes.forall(foe =>
          Tactic.Squads.Decisions.RetreatForbidden.released(foeMain(foe), homeRadius, ourDist(foe))) &&
        foes.forall(foe =>
          Tactic.Squads.Decisions.RetreatForbidden.releasedNatural(foeNatural(foe), homeRadius)),
      initial = false)
  }

  /**
   * PvP proxy defense (owner order): with few hulls home and an enemy
   * ground army already inside our natural-choke radius, every warrior
   * stands - a retreat here donates the main while the proxy still
   * builds up. Frame-memoized like HomeStand: the radius never moves
   * mid-game, so every reader shares one ground-path walk per frame.
   */
  private var proxyFrame: Int = Int.MinValue
  private var proxyHeld: Boolean = false

  def proxyDefense(): Boolean = {
    // Ladder homes only: UMS micro maps have no main to answer for, and
    // their duels are always joined, never judged.
    if (With.umsMode) return false

    if (With.frame != proxyFrame) {
      proxyFrame = With.frame
      proxyHeld = With.geography.ourBases.nonEmpty && Enemy.protoss() && {
        val mainHeart = With.geography.ourMain.heart.center
        ProxyDefense.holdsGround(
          enemyIsProtoss = true,
          ourWarriors = With.units.ours.count(u => u.alive && u.unitClass.isWarrior),
          nearestFoeMainTiles = Maff.minBy(With.units.enemy.view.filter(e =>
            e.alive && e.likelyStillThere && ! e.flying && e.unitClass.isWarrior && e.unitClass.attacksGround))(
            _.pixel.groundPixels(mainHeart))
            .map(_.pixel.groundPixels(mainHeart) * Maff.inv32).getOrElse(Double.PositiveInfinity),
          homeRadiusTiles = Information.Geography.Distances.naturalChokeToMainBase())
      }
    }

    proxyHeld
  }
}
