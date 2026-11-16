package Micro.Targeting

import Information.Geography.Chokes
import Information.Geography.Types.Edge
import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.{Pixel, Tile}
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}

/**
 * Terran wall-in detector (pure gate plus game-aware read): a Terran
 * Supply Depot and Barracks sitting inside two tiles of the enemy
 * natural choke is a blockade - but a blockade is not a seal. A
 * flattened depot, a misbuilt rax or a ramp left open still lets the
 * army through, so the trigger alone only means "suspected". Sealed
 * means verified: no ground path from our side to their natural.
 * Callers that must act on masonry (breaching, containing, ferrying
 * over it) read sealed; callers that probe use blocked. Tiles in,
 * tiles out; the game-aware read feeds numbers, never verdicts.
 */
object TerranWallIn {

  /** Wall pieces this close to the choke count as the blockade, in tiles. */
  val chokeRadiusTiles: Double = 2.0

  /** Both halves must sit in the lane: a lone depot is a depot, not a wall. */
  def hasWallPair(depotNear: Boolean, raxNear: Boolean): Boolean =
    depotNear && raxNear

  def nearChoke(distTiles: Double): Boolean =
    distTiles <= chokeRadiusTiles

  /** Sealed means no ground path around it: a leaky pile still lets the army through. */
  def sealedOff(pathExists: Boolean): Boolean =
    ! pathExists

  def shouldTreatAsWall(depotNearChoke: Boolean, raxNearChoke: Boolean, pathExists: Boolean): Boolean =
    hasWallPair(depotNearChoke, raxNearChoke) && sealedOff(pathExists)

  /** A wall piece: Terran depot or rax, alive and standing. */
  def isWallPiece(enemy: UnitInfo): Boolean =
    enemy.unitClass.isBuilding && (Terran.SupplyDepot(enemy) || Terran.Barracks(enemy))

  /** Pixels to tiles against the nearest choke tile (wide chokes included). */
  def distToChokeTiles(buildingPixel: Pixel, choke: Edge): Double = {
    val tiles = choke.tiles
    if (tiles.isEmpty) {
      return buildingPixel.pixelDistance(choke.pixelCenter) * Maff.inv32
    }

    Maff.minBy(tiles)(t => t.center.pixelDistanceSquared(buildingPixel))
      .map(t => t.center.pixelDistance(buildingPixel) * Maff.inv32)
      .getOrElse(Double.PositiveInfinity)
  }

  /** Suspected wall pieces at the enemy natural choke, if we have the read. */
  def wallPieces: Seq[UnitInfo] = {
    Chokes.enemyNaturalChoke.toSeq.flatMap(choke =>
      With.units.enemy.view.filter(e =>
        e.alive
          && e.likelyStillThere
          && isWallPiece(e)
          && nearChoke(distToChokeTiles(e.pixel, choke))).toSeq)
  }

  /** Trigger only: both halves sit in the lane. */
  def blocked: Boolean = {
    val pieces = wallPieces
    pieces.exists(Terran.SupplyDepot) && pieces.exists(Terran.Barracks)
  }

  /**
   * Trigger plus verification from the given tile: no ground path from
   * there to their natural means the blockade is a seal. Needs the
   * natural read; without it a suspected wall stays unverified.
   */
  def isSealed(from: Tile): Boolean = {
    if ( ! from.valid) return false
    if ( ! blocked) return false

    With.scouting.enemyNatural.exists(base =>
      ! With.paths.groundPathExists(from, base.townHallTile))
  }

  /** Convenience for a unit standing in front of it. */
  def isSealed(unit: FriendlyUnitInfo): Boolean =
    unit.tile.valid && isSealed(unit.tile)
}
