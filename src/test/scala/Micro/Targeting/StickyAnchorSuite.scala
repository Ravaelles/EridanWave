package Micro.Targeting

import org.scalatest.funsuite.AnyFunSuite

class StickyAnchorSuite extends AnyFunSuite {

  test("the first pick anchors") {
    assert(StickyAnchor.next(None, anchorAlive = true, anchorLegal = true, pickId = 7).contains(7))
  }

  test("the held incumbent stays anchored") {
    assert(StickyAnchor.next(Some(7), anchorAlive = true, anchorLegal = true, pickId = 7).contains(7))
  }

  test("a dead anchor releases to the new pick") {
    assert(StickyAnchor.next(Some(7), anchorAlive = false, anchorLegal = false, pickId = 9).contains(9))
  }

  test("a legal-but-beaten anchor loses fairly") {
    assert(StickyAnchor.next(Some(7), anchorAlive = true, anchorLegal = true, pickId = 9).contains(9))
  }

  test("an illegal-but-live anchor survives the forced jump") {
    // Kiting out of reach for a frame must not move the anchor: the
    // jump is a loan, the re-legalized incumbent keeps its bonus.
    assert(StickyAnchor.next(Some(7), anchorAlive = true, anchorLegal = false, pickId = 9).contains(7))
  }

  test("a gone anchor on an empty board still releases") {
    assert(StickyAnchor.next(Some(7), anchorAlive = false, anchorLegal = false, pickId = 9).contains(9))
  }
}
