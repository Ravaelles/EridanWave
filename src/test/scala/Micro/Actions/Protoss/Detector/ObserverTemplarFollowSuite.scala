package Micro.Actions.Protoss.Detector

import org.scalatest.funsuite.AnyFunSuite

class ObserverTemplarFollowSuite extends AnyFunSuite {

  test("the senior eye follows a blade inside fifty ground tiles of the main") {
    assert(ObserverTemplarFollow.inFollowRange(0.0))
    assert(ObserverTemplarFollow.inFollowRange(50.0))
    assert( ! ObserverTemplarFollow.inFollowRange(50.1))
  }

  test("the follow releases six tiles past the entry") {
    assert( ! ObserverTemplarFollow.released(50.0))
    assert( ! ObserverTemplarFollow.released(56.0))
    assert(ObserverTemplarFollow.released(56.1))
  }

  test("the latch holds inside the band and drops past it") {
    val eye = 900101
    assert(ObserverTemplarFollow.followLatched(eye, 40.0))
    assert(ObserverTemplarFollow.followLatched(eye, 53.0))
    assert( ! ObserverTemplarFollow.followLatched(eye, 57.0))
    assert( ! ObserverTemplarFollow.followLatched(eye, 53.0))
  }
}
