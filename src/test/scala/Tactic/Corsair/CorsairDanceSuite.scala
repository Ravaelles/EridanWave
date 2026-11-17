package Tactic.Corsair

import org.scalatest.funsuite.AnyFunSuite

class CorsairDanceSuite extends AnyFunSuite {

  test("a cooling Corsair far from its duel holds standoff") {
    assert(CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 12,
      distTiles     = 6.0,
      wound01       = 0.0,
      sporeCover    = true))
  }

  test("a ready Corsair goes for the kill instead of dancing") {
    assert( ! CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 0,
      distTiles     = 6.0,
      wound01       = 0.0,
      sporeCover    = true))
  }

  test("ten or fewer frames left means no dance, eleven means dance") {
    assert( ! CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 10,
      distTiles     = 6.0,
      wound01       = 0.0,
      sporeCover    = true))
    assert(CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 11,
      distTiles     = 6.0,
      wound01       = 0.0,
      sporeCover    = true))
  }

  test("a duel means cooling down or a recently started attack") {
    assert(CorsairDanceToOverlord.dueling(cooldownLeft = 5, framesSinceAttack = 1000))
    assert(CorsairDanceToOverlord.dueling(cooldownLeft = 0, framesSinceAttack = 10))
    assert( ! CorsairDanceToOverlord.dueling(cooldownLeft = 0, framesSinceAttack = 1000))
  }

  test("an ongoing attack on a live victim in range is never interrupted") {
    assert(CorsairDanceToOverlord.keepsAttacking(hasCommittedTarget = true, targetAlive = true, inRange = true))
    assert( ! CorsairDanceToOverlord.keepsAttacking(hasCommittedTarget = false, targetAlive = true, inRange = true))
    assert( ! CorsairDanceToOverlord.keepsAttacking(hasCommittedTarget = true, targetAlive = false, inRange = true))
    assert( ! CorsairDanceToOverlord.keepsAttacking(hasCommittedTarget = true, targetAlive = true, inRange = false))
  }

  test("a close Corsair goes for the kill instead of dancing") {
    assert( ! CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 12,
      distTiles     = 2.0,
      wound01       = 0.0,
      sporeCover    = true))
  }

  test("wounds raise the dance edge from 3 up to 5 tiles") {
    assert(CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 12,
      distTiles     = 5.1,
      wound01       = 1.0,
      sporeCover    = true))
    assert( ! CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 12,
      distTiles     = 4.9,
      wound01       = 1.0,
      sporeCover    = true))
    assert(CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 12,
      distTiles     = 4.9,
      wound01       = 0.0,
      sporeCover    = true))
  }

  test("no spore cover means no dance - run the overlord down") {
    assert( ! CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 30,
      distTiles     = 6.0,
      wound01       = 0.0,
      sporeCover    = false))
    assert( ! CorsairDanceToOverlord.shouldDance(
      cooldownLeft  = 30,
      distTiles     = 3.0,
      wound01       = 1.0,
      sporeCover    = false))
  }

  test("relocation is deterministic per unit and bucket") {
    assert(CorsairRelocateWhenIdle.relocateOffset(7, 3, 12) == CorsairRelocateWhenIdle.relocateOffset(7, 3, 12))
    assert(CorsairRelocateWhenIdle.relocateOffset(7, 3, 12) != CorsairRelocateWhenIdle.relocateOffset(7, 4, 12))
  }

  test("relocation offsets stay within the requested span") {
    Seq.tabulate(20)(bucket => CorsairRelocateWhenIdle.relocateOffset(7, bucket, 12)).foreach(offset => {
      assert(offset._1 >= -12 && offset._1 <= 12)
      assert(offset._2 >= -12 && offset._2 <= 12)
    })
  }
}
