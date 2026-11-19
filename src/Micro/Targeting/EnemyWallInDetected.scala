package Micro.Targeting

import Lifecycle.With
import Mathematics.Maff
import Micro.Agency.Commander
import ProxyBwapi.Orders
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitInfo.FriendlyUnitInfo
import ProxyBwapi.UnitInfo.UnitInfo

/**
 * Detected Terran wall-in fight (thin seam over pure predicates): when a
 * Depot-plus-Rax sits on the enemy natural choke, zealots chew the Depot
 * nearest our cluster instead of milling in front of it - a shared pick
 * keeps the whole line on one piece instead of each hull re-picking.
 * Ranged warriors hold outside the wall instead of walking in: they
 * shoot what comes out while the blades do the chewing.
 * Only while healthy enough to trade: below 31 hit points the hull
 * walks home instead, and with tanks on the map and shields at ten or
 * less it yields too - a weak hull trades the line for nothing.
 */
object EnemyWallInDetected {

  /** Fight the wall down to this many red hit points, no lower. */
  val fightHp: Int = 31

  /** Yield to tanks at or below this many shields. */
  val yieldShieldHp: Int = 10

  /** Ranged hulls hold instead of marching inside this band, in tiles. */
  val holdWallTiles: Double = 8.0

  def shouldHoldVsWall(distTiles: Double): Boolean =
    distTiles <= holdWallTiles

  def shouldFightDepot(hitPoints: Int): Boolean =
    hitPoints >= fightHp

  def shouldYieldToTanks(enemyHasTanks: Boolean, shields: Int): Boolean =
    enemyHasTanks && shields <= yieldShieldHp

  /** Nearest standing depot to our cluster: one shared victim, no flap. */
  def pickDepot[T](marks: Seq[(T, Double)]): Option[T] =
    Maff.minBy(marks)(_._2).map(_._1)

  /**
   * Repair snipe (pure flags for the suite): an SCV visibly repairing
   * the depot dies first - free repairs out-damage the chew. The blade
   * must see theSCV; blind swings at fogged memories stay on the depot.
   */
  def shouldSnipeRepairer(visible: Boolean, isScv: Boolean, repairingDepot: Boolean): Boolean =
    visible && isScv && repairingDepot

  def check(unit: FriendlyUnitInfo): Boolean = {
    if ( ! unit.unitClass.isWarrior || unit.flying) return false
    if ( ! TerranWallIn.blocked) return false
    // Outgunned line holds off the wall for the rest of the game.
    if (WallInBan.banned()) return false

    if ( ! shouldFightDepot(unit.hitPoints)) return false

    if (shouldYieldToTanks(
      With.units.enemy.exists(e => e.alive && (Terran.SiegeTankSieged(e) || Terran.SiegeTankUnsieged(e))),
      unit.shieldPoints)) return false

    val centroid = unit.squad.map(_.centroidGround).getOrElse(unit.pixel)
    val depot = pickDepot(
      With.units.enemy.view.filter(e =>
        e.alive && Terran.SupplyDepot(e)).map(d =>
        (d, d.pixelDistanceCenter(centroid))).toSeq)
    if (depot.isEmpty) return false

    if (unit.isAny(Protoss.Zealot)) {
      // A dueling blade keeps its duel: only the idle marchers take the
      // depot, so the line converges on one piece instead of flip-flopping
      // between chewing and chasing. A Barracks duel is not a duel: the
      // doctrine is depot-only, so a blade chewing the Rax re-points.
      if (unit.agent.toAttack.exists(t =>
        t.alive && ! Terran.Barracks(t) && unit.matchups.targetsInRange.exists(_.id == t.id))) return false

      // Repairing SCVs die first: free repairs out-damage the chew, and
      // the SCV stands on the depot the blade already holds.
      val scv = With.units.enemy.view.find(e =>
        shouldSnipeRepairer(
          e.visible,
          Terran.SCV(e),
          e.alive && e.order == Orders.Repair && e.orderTarget.contains(depot.get)))
      scv.foreach(victim => {
        unit.agent.toAttack = Some(victim)
        unit.agent.act("EnemyWallInRepair")
        Commander.attack(unit, victim)
      })
      if (scv.isDefined) return true

      unit.agent.toAttack = depot
      unit.agent.act("EnemyWallIn")
      Commander.attack(unit, depot.get)

      true
    } else if ( ! unit.unitClass.melee && ! unit.isAny(Protoss.Reaver)) {
      // Ranged hulls never walk into the wall: dueling ones keep their
      // duel, the idle ones hold the line and shoot what comes out.
      if (unit.matchups.targetsInRange.nonEmpty) return false
      if ( ! shouldHoldVsWall(unit.pixelDistanceEdge(depot.get) * Maff.inv32)) return false

      unit.agent.act("WallStand")
      Commander.hold(unit)

      true
    } else false
  }
}
