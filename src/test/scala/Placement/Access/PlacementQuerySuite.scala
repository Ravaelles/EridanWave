package Placement.Access

import Mathematics.Points.Tile
import org.scalatest.funsuite.AnyFunSuite

class PlacementQuerySuite extends AnyFunSuite {

  test("the main choke stays five tiles clear of everything") {
    assert(PlacementQuery.mainChokeClearTiles == 5)
  }

  test("keep-off anchors accumulate per choke") {
    val query = new PlacementQuery()
    val anchor = Tile(10, 10)
    query.requireFar(anchor, 4)
    query.requireFar(anchor, 5)
    assert(query.requirements.farTiles == Seq((anchor, 4), (anchor, 5)))
  }
}
