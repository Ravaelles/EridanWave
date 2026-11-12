package Placement.Walls

import Information.Geography.Types.Zone
import Placement.Walls.WallSpans.{TerrainGas, TerrainHall}

/** Candidate and time budget shared by the search loop and each permutation
  * try: the loop decides when to stop, the try spends the budget.
  */
final class WallSearchBudget(
    val maxCandidatesPerPermutation: Int,
    val maxCandidatesPerConstraint : Int,
    var candidatesThisConstraint   : Int,
    var candidatesThisPermutation  : Int,
    private val expired            : () => Boolean) {
  def searchedEnough: Boolean =
    expired() || candidatesThisConstraint >= maxCandidatesPerConstraint || candidatesThisPermutation >= maxCandidatesPerPermutation
}

/** Enumerates ways to place the required buildings of each wall constraint,
  * publishing every arrangement that passes the wall checks. One permutation
  * is tried by [[WallPlacementTry]]; filler placement and the final selection
  * stay in [[WallFinder]].
  */
class WallPlacementSearch(finder: WallFinder, zone: Zone, constraints: Seq[WallConstraint], startedNanos: Long) {

  import finder._

  private val searchDeadlineNanos = startedNanos + 45000000000L
  private val budget = new WallSearchBudget(
    maxCandidatesPerPermutation = 64,
    maxCandidatesPerConstraint  = 256,
    candidatesThisConstraint    = 0,
    candidatesThisPermutation   = 0,
    expired                     = () => searchExpired)

  private var constraintIndex: Int = 0

  private def searchExpired: Boolean = System.nanoTime() >= searchDeadlineNanos

  private def constraint: WallConstraint = wallInProgress.constraint

  private def pickNextConstraint(): Unit = {
    wallInProgress.buildings.clear()
    wallInProgress.constraint = constraints(constraintIndex)
    wallInProgress.gap        = None
    wallInProgress.hallway    = Seq.empty
    constraintIndex += 1
  }

  // Overall plan:
  // - Identify starting boundary
  // - Identify ending boundaries
  //   - Terrain-Terrain
  //   - Terrain-Gas
  //   - Terrain-Hall
  // For each constraint:
  // - For each starting boundary:
  //  - For each ending boundary:
  //    - If this line contains any other starting/ending boundaries, skip
  //    - If this line's max width exceeds the max width of our buildings + gap size, skip
  //    - If this line's max height exceeds the max height of our buildings + gap size, skip
  //    - Fill queue with buildings to place
  //    - For each place we could place the first building adjacent to the first tile: (LATER: Select cleverly to fill gaps)
  //      - Try placing each building adjacent to it or using our gap budget
  def run(): Unit = {
    if (constraints.isEmpty)        return
    if (zone.exitOriginal.isEmpty)  return

    while (constraintIndex < constraints.length && ! searchExpired) {
      pickNextConstraint()
      val permutations = constraint.buildings.permutations.toVector.distinct
      metrics.permutations += permutations.length
      val startingBoundary = zone.wallPerimeterEntrance
      permutations.iterator.takeWhile(_ => ! searchExpired && budget.candidatesThisConstraint < budget.maxCandidatesPerConstraint).foreach(buildings => {
        budget.candidatesThisPermutation = 0
        new WallPlacementTry(finder, constraint, boundaryEnd, budget).place(startingBoundary, buildings, constraint.gapTiles)
      })
    }
  }

  private def boundaryEnd = constraint.span match {
    case TerrainGas   => zone.wallPerimeterGas
    case TerrainHall  => zone.wallPerimeterHall
    case _            => zone.wallPerimeterExit
  }
}
