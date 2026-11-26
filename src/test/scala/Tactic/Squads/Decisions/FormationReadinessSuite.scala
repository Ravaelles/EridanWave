package Tactic.Squads.Decisions

import org.scalatest.funsuite.AnyFunSuite

class FormationReadinessSuite extends AnyFunSuite {

  import FormationReadiness._

  test("no enemy near means no line to form") {
    assert(next(Chaotic, minGapTiles = 30.0, spreadTiles = 9.0, contactAny = false, retreating = false) == Chaotic)
    assert(next(Forming, minGapTiles = 30.0, spreadTiles = 9.0, contactAny = false, retreating = false) == Chaotic)
    assert(next(Formed, minGapTiles = 30.0, spreadTiles = 1.0, contactAny = false, retreating = false) == Chaotic)
  }

  test("a scattered pack facing the enemy forms") {
    assert(next(Chaotic, minGapTiles = 8.0, spreadTiles = 9.0, contactAny = false, retreating = false) == Forming)
    assert(next(Chaotic, minGapTiles = 12.0, spreadTiles = 9.0, contactAny = false, retreating = false) == Forming)
  }

  test("an already tight pack facing the enemy stands formed") {
    assert(next(Chaotic, minGapTiles = 8.0, spreadTiles = 1.0, contactAny = false, retreating = false) == Formed)
    assert(next(Forming, minGapTiles = 8.0, spreadTiles = 1.5, contactAny = false, retreating = false) == Formed)
  }

  test("the line holds formed through small breathing, reforms past three") {
    assert(next(Formed, minGapTiles = 8.0, spreadTiles = 2.5, contactAny = false, retreating = false) == Formed)
    assert(next(Formed, minGapTiles = 8.0, spreadTiles = 3.01, contactAny = false, retreating = false) == Forming)
  }

  test("sight flicker at the edge never flaps the line") {
    assert(next(Forming, minGapTiles = 13.4, spreadTiles = 9.0, contactAny = false, retreating = false) == Forming)
    assert(next(Forming, minGapTiles = 13.51, spreadTiles = 9.0, contactAny = false, retreating = false) == Chaotic)
  }

  test("contact freezes the state: the duel owns the frame, never the drill") {
    assert(next(Forming, minGapTiles = 1.0, spreadTiles = 9.0, contactAny = true, retreating = false) == Forming)
    assert(next(Chaotic, minGapTiles = 1.0, spreadTiles = 9.0, contactAny = true, retreating = false) == Chaotic)
  }

  test("retreat dissolves the line back to chaos") {
    assert(next(Forming, minGapTiles = 8.0, spreadTiles = 9.0, contactAny = false, retreating = true) == Chaotic)
    assert(next(Formed, minGapTiles = 8.0, spreadTiles = 1.0, contactAny = false, retreating = true) == Chaotic)
  }

  test("the rear holds, the middle falls back to it, the lost wait") {
    assert(backStepTiles(myGapTiles = 10.0, rearGapTiles = 10.0) == 0.0)
    assert(backStepTiles(myGapTiles = 9.0, rearGapTiles = 10.0) == 0.0)
    assert(backStepTiles(myGapTiles = 8.0, rearGapTiles = 10.0) == 0.5)
    assert(backStepTiles(myGapTiles = 4.0, rearGapTiles = 10.0) == 4.5)
    assert(backStepTiles(myGapTiles = 0.5, rearGapTiles = 10.0) == 0.0)
  }

  test("the forced drill holds Forming for fifteen frames, then stands formed") {
    assert(enforceMinForm(Forming, Formed, framesInForming = 0) == Forming)
    assert(enforceMinForm(Forming, Formed, framesInForming = 14) == Forming)
    assert(enforceMinForm(Forming, Formed, framesInForming = 15) == Formed)
    assert(enforceMinForm(Forming, Forming, framesInForming = 0) == Forming)
    assert(enforceMinForm(Chaotic, Formed, framesInForming = 0) == Formed)
  }
}
