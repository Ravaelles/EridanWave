package Micro.Targeting

import Information.Geography.Chokes
import Information.Geography.Types.Edge
import Lifecycle.With
import Mathematics.Maff
import Mathematics.Points.{Pixel, Tile}
import ProxyBwapi.Races.Terran
import ProxyBwapi.UnitInfo.{FriendlyUnitInfo, UnitInfo}
import Utilities.Time.Seconds

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

  /**
   * Sealed-breach reach, in tiles: the user's wall rule chews a piece
   * this close to the enemy natural choke when the hull stands this
   * close to the piece itself - a battered column still knocks, because
   * a sealed wall leaves nothing else to fight.
   */
  val sealedBreachTiles: Double = 6.0

  /** Quiet gun on the sealed path, in frames: three seconds without a swing. */
  val quietSealedFrames: Int = Seconds(3)()

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

  /**
   * The user's wall rule (pure numbers, no game access): close to the
   * piece, quiet gun past three seconds, the piece inside the breach
   * reach of the choke, and the lane verified sealed. No shield term:
   * a battered column at a sealed wall has nothing else to fight.
   */
  def shouldBreakSealed(
    unitToPieceTiles: Double,
    framesSinceAttack: Int,
    pieceToChokeTiles: Double,
    sealedWall: Boolean): Boolean =
    unitToPieceTiles <= sealedBreachTiles &&
      framesSinceAttack > quietSealedFrames &&
      pieceToChokeTiles <= sealedBreachTiles &&
      sealedWall

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
  def wallPieces: Seq[UnitInfo] =
    wallPiecesNear(chokeRadiusTiles)

  /** Wall pieces inside the given reach of the enemy natural choke, in tiles. */
  def wallPiecesNear(reachTiles: Double): Seq[UnitInfo] = {
    Chokes.enemyNaturalChoke.toSeq.flatMap(choke =>
      With.units.enemy.view.filter(e =>
        e.alive
          && e.likelyStillThere
          && isWallPiece(e)
          && distToChokeTiles(e.pixel, choke) <= reachTiles).toSeq)
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

  /**
   * Seal verification without the tight trigger: no ground path from
   * the given tile to their natural. The user's rule calls this only
   * while wall pieces already sit inside the breach reach, so the pair
   * gates itself - a sealed lane with masonry on it is a wall-in no
   * matter how the pieces were first noticed.
   */
  def isSealedBreach(from: Tile): Boolean = {
    if ( ! from.valid) return false

    With.scouting.enemyNatural.exists(base =>
      ! With.paths.groundPathExists(from, base.townHallTile))
  }

  /** Convenience for a unit standing in front of it. */
  def isSealed(unit: FriendlyUnitInfo): Boolean =
    unit.tile.valid && isSealed(unit.tile)
}
