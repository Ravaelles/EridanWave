package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class BoardStalledSuite extends AnyFunSuite {

  test("a stalled hull boards the waiting ride instead of milling") {
    assert(BoardStalled.shouldBoard(stalledShot = true, staticFoeInGun = true, rideInReach = true))
  }

  test("a progressing duel keeps shooting, never boards") {
    assert( ! BoardStalled.shouldBoard(stalledShot = false, staticFoeInGun = true, rideInReach = true))
  }

  test("a stalled mobile duel keeps dueling, never boards") {
    assert( ! BoardStalled.shouldBoard(stalledShot = true, staticFoeInGun = false, rideInReach = true))
  }

  test("a stalled duel without a ride in reach keeps dueling") {
    assert( ! BoardStalled.shouldBoard(stalledShot = true, staticFoeInGun = true, rideInReach = false))
  }
}
