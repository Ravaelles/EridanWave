package Micro.Targeting

import Lifecycle.With
import Mathematics.Maff
import ProxyBwapi.Races.{Protoss, Terran}
import Utilities.Latch

/**
 * Wall-in ban (pure thresholds plus one game-wide latch): dragoons
 * trade with sieged tanks at five to one - below that the line holds
 * off the wall for the rest of the game instead of feeding it a
 * dragoon at a time. A maxed bank or a mining stockpile releases the
 * ban (brute force outvalues the ratio); the latch re-arms if supply
 * crashes back through the lower edge. Foes without tanks never trip
 * it, so non-Terran breaches read exactly as before.
 */
object WallInBan {

  /** Fight while dragoons outnumber sieged tanks five to one. */
  val dragoonsPerTank: Int = 5

  /** A maxed line brute-forces the wall regardless of the ratio. */
  val releaseSupply: Int = 195

  /** A banked line buys its way through regardless of the ratio. */
  val releaseMinerals: Int = 1200

  /** Re-arm the ban check once supply falls back through here. */
  val relatchSupply: Int = 165

  def outgunned(dragoons: Int, siegedTanks: Int): Boolean =
    dragoons * dragoonsPerTank < siegedTanks

  def released(supplyUsed: Int, minerals: Int): Boolean =
    supplyUsed >= releaseSupply || minerals >= releaseMinerals

  def relatch(supplyUsed: Int): Boolean =
    supplyUsed <= relatchSupply

  private val bannedLatch = new Latch

  def banLatched(outgunnedNow: Boolean, supplyUsed: Int, minerals: Int): Boolean =
    bannedLatch(
      0,
      fires    = outgunnedNow && relatch(supplyUsed),
      releases = released(supplyUsed, minerals),
      initial  = false)

  /** Game-coupled read: count the line, price the tanks, obey the latch. */
  def banned(): Boolean = {
    val dragoons = With.units.ours.view.count(u => u.alive && Protoss.Dragoon(u))
    val sieged = With.units.enemy.view.count(e => e.alive && Terran.SiegeTankSieged(e))
    val supply = With.self.supplyUsed200
    val minerals = With.self.minerals
    banLatched(
      outgunned(dragoons, sieged),
      supply,
      minerals)
  }

  /** Nearest enemy building to the hull, in tiles, or infinite ground. */
  def nearestEnemyBuildingTiles(pixel: Mathematics.Points.Pixel): Double =
    Maff.minBy(With.units.enemy.view.filter(e =>
      e.alive && e.unitClass.isBuilding))(
      _.pixelDistanceCenter(pixel))
      .map(_.pixelDistanceCenter(pixel) * Maff.inv32)
      .getOrElse(Double.PositiveInfinity)
}
