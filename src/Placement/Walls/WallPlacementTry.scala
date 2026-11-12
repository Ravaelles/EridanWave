package Placement.Walls

import Mathematics.Maff
import Mathematics.Points.Tile
import Mathematics.Shapes.{Pylons, RoundedBox}
import Placement.Walls.WallProblems.{GapTooNarrow, GapTooWide, InsufficientlyTight, UnpoweredByWall}
import ProxyBwapi.UnitClasses.UnitClass

/** Tries one permutation of a constraint's buildings against the entrance
  * boundary: every building placement and gap budget that passes the wall
  * checks is published, and the try reports when the permutation's candidate
  * budget is spent. The loop over constraints and permutations lives in
  * [[WallPlacementSearch]]; the candidate budget in [[WallSearchBudget]].
  */
class WallPlacementTry(
    finder: WallFinder,
    constraint: WallConstraint,
    boundaryEnd: Seq[Tile],
    budget: WallSearchBudget) {

  import finder._

  def place(horizon: Seq[Tile], buildingsLeft: Seq[UnitClass], gapsLeft: Int): Boolean = {
    if (buildingsLeft.isEmpty) {
      return publishCompleteWall(horizon, gapsLeft)
    }

    // For each tile in the boundary,
    //   for each way to place a tile adjacent to it,
    //     for each gap size still available,
    //       Try placing the next building there,
    //       then repeat recursively.
    //       If we're out of buildings to place,
    //       and the whole arrangement is legal,
    //       publish it.
    //
    // Places around the target tile to try at each gap size:
    //    1111
    //    0000
    //  10----01
    //  10----01
    //  10---+01
    //    0000
    //    1111
    //
    val building  = buildingsLeft.head
    val width     = building.tileWidthPlusAddon
    val height    = building.tileHeight
    horizon.exists(horizonTile => {
      metrics.tilesConsidered += 1
      RoundedBox(width + 2, height + 2).exists(relative =>
        (0 to gapsLeft).exists(gapsUsed => {
          val gapOffset    = WallAdjacency.gapOffset(relative, width, gapsUsed)
          val buildingTile = horizonTile
            .subtract(width, height)
            .add(relative)
            .add(gapOffset)
          lazy val alleys = wallInProgress.buildings.lastOption.map(b => WallAdjacency.alleys(
            WallPlacement.Rect(b._1.x, b._1.y, b._2.tileWidthPlusAddon, b._2.tileHeight),
            WallAdjacency.marginsOf(b._2),
            WallPlacement.Rect(buildingTile.x, buildingTile.y, width, height),
            WallAdjacency.marginsOf(building))).getOrElse(WallAdjacency.none)

          if ( ! canPlace(width, height, buildingTile, countMetrics = true)) {
            false
          }

          // Gaps must meet tightness criterion
          else if (gapsUsed == 0
            && (constraint.blocksUnit.width < 32 || constraint.blocksUnit.height < 32) // Don't even bother checking for big units
            && alleys.leaksTo(constraint.blocksUnit.width, constraint.blocksUnit.height)) {
            return fail(wallInProgress, InsufficientlyTight)
          }

          // Success! Place the next building.
          else {
            // Apply changes
            wallInProgress.buildings += ((buildingTile, building))

            // Preserve state for later reversion
            val gapBefore = wallInProgress.gap
            if (gapBefore.isEmpty && gapsUsed > 0) {
              wallInProgress.gap = Some(horizonTile.add(gapOffset.direction.x, gapOffset.direction.y))
            }
            // Test changes
            val output = place(building.tileAreaPlusAddon.tilesAtEdge.map(buildingTile.add), buildingsLeft.drop(1), gapsLeft - gapsUsed)
            // Revert changes
            wallInProgress.gap = gapBefore
            wallInProgress.buildings.remove(wallInProgress.buildings.length - 1)
            output
          }}))})
  }

  /** Records the arrangement if it blocks and powers correctly. Returns true
    * once this permutation has produced enough candidates to stop searching it.
    */
  private def publishCompleteWall(horizon: Seq[Tile], gapsLeft: Int): Boolean = {
    // A gap recorded while placing a building must not be covered by that building.
    if (wallInProgress.gap.exists(gap => wallInProgress.buildings.exists(b => b._2.tileAreaPlusAddon.add(b._1).contains(gap)))) {
      return fail(wallInProgress, GapTooNarrow)
    }

    // TODO: If gapsLeft is zero enforce constraint.blocksUnit

    // Require the gap to meet constraints
    if (horizon.forall(t => boundaryEnd.forall(_.tileDistanceManhattan(t) > 1 + gapsLeft))) {
      return fail(wallInProgress, GapTooWide)
    }

    /* Disabling because we're changing the way we iterate to avoid having to do this check, at a cost of rejecting walls where the gap is at the far end
    // Use Chebyshev distance to check gap size because cater-corner diagonals are not acceptable
    if (horizon.exists(t => boundaryEnd.exists(_.tileDistanceChebyshev(t) <= gapsLeft))) {
      metrics.gapTooNarrow += 1

      return false
    }
    */
    // If the wall has a Pylon, then it must power the other buildings.
    val pylons = wallInProgress.buildings.filter(_._2.powers)
    if (pylons.nonEmpty) {
      val unpowered = wallInProgress.buildings.find(b => b._2.requiresPsi && ! pylons.exists(p => Pylons.powers(p._1, b._1, b._2)))
      if (unpowered.isDefined) {
        return fail(wallInProgress, UnpoweredByWall)
      }
    }

    if (wallInProgress.gap.isEmpty && gapsLeft > 0) {
      val gapCandidates       = horizon.flatMap(_.adjacent4).toSet.filter(t => t.walkable && ! wallInProgress.buildings.exists(b => b._2.tileAreaPlusAddon.add(b._1).contains(t)))
      wallInProgress.gap      = Maff.minBy(gapCandidates)(t => boundaryEnd.map(_.tileDistanceChebyshev(t)).min)
      if (wallInProgress.gap.forall(t => boundaryEnd.map(_.tileDistanceChebyshev(t)).min > gapsLeft)) {
        return fail(wallInProgress, GapTooWide)
      }
    }

    wallInProgress.score = scoreWall(wallInProgress)
    wallsAcceptable += new Wall(wallInProgress)
    budget.candidatesThisConstraint += 1
    budget.candidatesThisPermutation += 1
    metrics.candidatesGenerated += 1

    budget.searchedEnough
  }
}
