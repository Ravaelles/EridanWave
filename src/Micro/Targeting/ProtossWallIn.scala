package Micro.Targeting

import Information.Geography.Chokes
import Lifecycle.With
import Mathematics.Maff
import Micro.Agency.Commander
import ProxyBwapi.Races.Protoss
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Tactic.Squads.Decisions.RetreatVerdict
import Utilities.Latch
import Utilities.Time.Minutes

/**
 * Protoss wall-in fight (thin seam over pure predicates): when three or
 * more Protoss buildings sit on the enemy natural choke early, the wall
 * is the fight - dragoons chew the building nearest the cluster instead
 * of milling in front of it, and a shared pick keeps the whole line on
 * one piece instead of each hull re-picking. A chewed hull steps off at
 * fifteen wound and comes back at thirteen (or with the bank behind it),
 * so the line grinds the wall down instead of donating dragoons one at
 * a time. Zealots are never claimed: blades have no business chewing
 * into cannons. Dark Templars keep their own brain.
 */
object ProtossWallIn {

  /** Wall pieces sit this close to the choke, in tiles. */
  val wallRadiusTiles: Double = 4.0

  /** This many buildings on the choke means a wall, not a base. */
  val enterBuildings: Int = 3

  /** Chew down to this many and the lane is open: mission over. */
  val releaseBuildings: Int = 1

  /** Step off the wall at this much wound (shields plus hull). */
  val withdrawWound: Int = 15

  /** Come back at this much wound: the 15/13 band never flaps. */
  val reenterWound: Int = 13

  /** A maxed line fights wounded: brute force outvalues the chew. */
  val fightSupply: Int = 180

  /** A banked line fights wounded: replacements outvalue shields. */
  val fightMinerals: Int = 1000

  /**
   * Hold and chew only this close to a wall piece, in tiles: marching
   * twenty tiles to a chew through live guns donates the hull before
   * it ever shoots. Out of reach the march owns the approach.
   */
  val maxEngageTiles: Double = 9.0

  def inReach(distTiles: Double): Boolean = distTiles <= maxEngageTiles

  /** Holders need shields up: below this the hull retreats instead of
    * standing under the guns. */
  val minHoldShields: Int = 20

  def shieldsHold(shieldPoints: Int): Boolean = shieldPoints >= minHoldShields

  /** Detection only runs this early, in frames: late walls are sieges. */
  def detectFrames: Int = Minutes(9)()

  def detected(buildingCount: Int, frame: Int): Boolean =
    buildingCount >= enterBuildings && frame <= detectFrames

  def released(buildingCount: Int, frame: Int): Boolean =
    buildingCount <= releaseBuildings || frame > detectFrames

  def overrideNow(supplyUsed: Int, minerals: Int): Boolean =
    supplyUsed >= fightSupply || minerals >= fightMinerals

  def shouldFightWall(withdrawing: Boolean, supplyUsed: Int, minerals: Int): Boolean =
    ! withdrawing || overrideNow(supplyUsed, minerals)

  private val missionLatch = new Latch
  private val woundLatch = new Latch

  def missionLatched(buildingCount: Int, frame: Int): Boolean =
    missionLatch(
      0,
      fires    = detected(buildingCount, frame),
      releases = released(buildingCount, frame),
      initial  = false)

  def woundLatched(unitId: Int, wound: Int): Boolean =
    woundLatch(
      unitId,
      fires    = wound >= withdrawWound,
      releases = wound <= reenterWound,
      initial  = false)

  /** Enemy Protoss buildings standing on the choke, alive and close. */
  def wallPiecesNear(choke: Information.Geography.Types.Edge): Seq[UnitInfo] =
    With.units.enemy.view.filter(e =>
      e.alive && e.unitClass.isBuilding && e.unitClass.isProtoss
        && TerranWallIn.distToChokeTiles(e.pixel, choke) <= wallRadiusTiles).toSeq

  /** The mission is on: detected once, held until the lane opens. */
  def active(): Boolean =
    Chokes.enemyNaturalChoke.exists(choke => {
      val count = wallPiecesNear(choke).size
      missionLatched(count, With.frame)
    })

  def woundOf(unit: FriendlyUnitInfo): Int =
    unit.unitClass.maxTotalHealth - unit.totalHealth

  def check(unit: FriendlyUnitInfo): Boolean = {
    if ( ! unit.unitClass.isWarrior || unit.flying) return false
    if ( ! Protoss.Dragoon(unit)) return false
    if ( ! active()) return false
    // Retreat owns the frame when it is held: the wall mission never
    // outvotes a retreat, it only grinds walls nobody is dying on. The
    // verdict counts units, never masonry.
    if (RetreatVerdict.held(unit.id, With.frame)) return false

    val centroid = unit.squad.map(_.centroidGround).getOrElse(unit.pixel)
    val pieces = Chokes.enemyNaturalChoke.map(wallPiecesNear).getOrElse(Seq.empty)
      .filter(piece => unit.pixelDistanceEdge(piece) * Maff.inv32 <= maxEngageTiles)
    // Out of reach the march owns the approach: holding or chewing
    // twenty tiles out fights the enemy army with a wall mission.
    if (pieces.isEmpty) return false

    val withdrawing = woundLatched(unit.id, woundOf(unit))
    if ( ! shouldFightWall(withdrawing, With.self.supplyUsed200, With.self.minerals)) {
      // Holders stand only unpressured and shielded: mobile guns on top
      // or collapsed shields lift the mission - the hull retreats
      // instead of donating itself under the wall. Static fire alone
      // never lifts it: grinding under it is the job.
      val pressedByUnits = unit.matchups.threatsInRange.exists(t => ! t.unitClass.isBuilding)
      if (pressedByUnits || ! shieldsHold(unit.shieldPoints)) return false

      unit.agent.act("ProtossWallHold")
      Commander.hold(unit)

      return true
    }

    Maff.minBy(pieces)(_.pixelDistanceCenter(centroid)).foreach(piece => {
      unit.agent.toAttack = Some(piece)
      unit.agent.act("ProtossWallIn")
      Commander.attack(unit, piece)
    })

    pieces.nonEmpty
  }
}
