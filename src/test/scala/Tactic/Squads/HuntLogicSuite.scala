package Tactic.Squads

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class HuntLogicSuite extends AnyFunSuite {

  private val home    = Pixel(100, 100)
  private val contact = Pixel(1000, 1000)
  private val farTile = Pixel(3000, 3000)

  test("no latch: chase the contact, ignore unseen tiles") {
    val (target, stamp) =
      HuntLogic.refresh(None, reached = false, expired = false,
        contact = Some((contact, 50)), setFrame = 0,
        unseen = Seq(farTile), now = 100)
    assert(target.contains(contact))
    assert(stamp == 100)
  }

  test("no latch and no contact: take the nearest unseen tile") {
    val (target, stamp) =
      HuntLogic.refresh(None, reached = false, expired = false,
        contact = None, setFrame = 0,
        unseen = Seq(farTile), now = 100)
    assert(target.contains(farTile))
    assert(stamp == 100)
  }

  test("no latch and nothing to hunt: stay without a target") {
    val (target, _) =
      HuntLogic.refresh(None, reached = false, expired = false,
        contact = None, setFrame = 0,
        unseen = Seq.empty, now = 100)
    assert(target.isEmpty)
  }

  test("latched hunt holds while nothing changed") {
    val (target, stamp) =
      HuntLogic.refresh(Some(home), reached = false, expired = false,
        contact = None, setFrame = 50,
        unseen = Seq(farTile), now = 100)
    assert(target.contains(home))
    assert(stamp == 50)
  }

  test("reaching the latch re-picks from what is known") {
    val (target, stamp) =
      HuntLogic.refresh(Some(home), reached = true, expired = false,
        contact = None, setFrame = 50,
        unseen = Seq(farTile), now = 100)
    assert(target.contains(farTile))
    assert(stamp == 100)
  }

  test("an expired latch is dropped even with nowhere new to go") {
    val (target, _) =
      HuntLogic.refresh(Some(home), reached = false, expired = true,
        contact = None, setFrame = 50,
        unseen = Seq.empty, now = 100)
    assert(target.isEmpty)
  }

  test("a contact newer than the latch supersedes it") {
    val (target, stamp) =
      HuntLogic.refresh(Some(home), reached = false, expired = false,
        contact = Some((contact, 80)), setFrame = 50,
        unseen = Seq(farTile), now = 100)
    assert(target.contains(contact))
    assert(stamp == 100)
  }

  test("a contact older than the latch does not steal it") {
    val (target, stamp) =
      HuntLogic.refresh(Some(home), reached = false, expired = false,
        contact = Some((contact, 40)), setFrame = 50,
        unseen = Seq(farTile), now = 100)
    assert(target.contains(home))
    assert(stamp == 50)
  }
}
