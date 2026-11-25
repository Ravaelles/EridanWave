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
 * Depot-plus-Rax sits on the enemy natural choke, the line chews the Depot
 * nearest our cluster instead of standing before it - a shared pick keeps
 * the whole line on one piece instead of each hull re-picking. Blades
 * and fresh guns alike: ranged warriors in range trade into the wall
 * instead of posing before it while the rally drags them back and forth
 * (PvT WallStand/Rallying flap). Only while the hull can trade: below 31
 * hit points it walks home instead, and under tanks only shields fresh
 * to thirteen or less attack - a spent hull under guns yields the frame
 * to the retreat instead of donating the line.
 */
object EnemyWallInDetected {

  /** Fight the wall down to this many red hit points, no lower. */
  val fightHp: Int = 31

  /** Fresh shields that still trade into the wall, as shield wound. */
  val attackShieldWound: Int = 13

  def shouldFightDepot(hitPoints: Int): Boolean =
    hitPoints >= fightHp

  /**
   * Take the wall (pure flags for the suite): without tanks always -
   * the wall is the game - and under tanks only on fresh shields, so a
   * blade or gun in range chews while it can still trade, and a spent
   * hull under guns retreats instead of posing.
   */
  def shouldTakeWall(enemyHasTanks: Boolean, shieldWound: Int): Boolean =
    ! enemyHasTanks || shieldWound <= attackShieldWound

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

    // Tanks (sieged or not) open the retreat question - but a hull in
    // range on fresh shields still chews instead of posing: the wound
    // decides, not the tanks alone.
    val enemyHasTanks = With.units.enemy.exists(e => e.alive && (Terran.SiegeTankSieged(e) || Terran.SiegeTankUnsieged(e)))
    if ( ! shouldTakeWall(enemyHasTanks, unit.shieldWound)) return false

    val centroid = unit.squad.map(_.centroidGround).getOrElse(unit.pixel)
    val depot = pickDepot(
      With.units.enemy.view.filter(e =>
        e.alive && Terran.SupplyDepot(e)).map(d =>
        (d, d.pixelDistanceCenter(centroid))).toSeq)
    if (depot.isEmpty) return false

    if (unit.unitClass.melee && ! unit.isAny(Protoss.Reaver)) {
      // A dueling blade keeps its duel: only the idle marchers take the
      // depot, so the line converges on one piece instead of flip-flopping
      // between chewing and chasing - templars on the hunt included, as
      // long as the hunt has nothing hittable (the brain only hunts on
      // an empty slot, so the depot assignment below sticks).
      // A Barracks duel is not a duel: the doctrine is depot-only, so a
      // blade chewing the Rax re-points.
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
      // Fresh guns chew with the blades: a hull in range with shields
      // to trade attacks the shared depot instead of holding before it -
      // the stand donated the volleys while the rally dragged the hull
      // back and forth. Dueling hulls keep their duel; spent hulls never
      // reach this leg (the wound gate above yields them to the retreat).
      if (unit.matchups.targetsInRange.nonEmpty) return false

      unit.agent.toAttack = depot
      unit.agent.act("EnemyWallIn")
      Commander.attack(unit, depot.get)

      true
    } else false
  }
}
