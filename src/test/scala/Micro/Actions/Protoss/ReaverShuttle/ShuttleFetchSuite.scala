package Micro.Actions.Protoss.ReaverShuttle

import org.scalatest.funsuite.AnyFunSuite

class ShuttleFetchSuite extends AnyFunSuite {

  private def fetch(
    hasTargets: Boolean = false,
    wantsPickup: Boolean = false,
    scarabTargeted: Boolean = false,
    endangered: Boolean = false,
    travelsFar: Boolean = false,
    pairIdle: Boolean = false,
    strandedFetch: Boolean = false,
    reserved: Boolean = false): Boolean =
    ShuttleFetch.shouldFetchReaver(
      hasTargets, wantsPickup, scarabTargeted,
      endangered, travelsFar, pairIdle, strandedFetch, reserved)

  test("a duelling hull is not a pickup unless it hails or is scarabbed") {
    assert( ! fetch(hasTargets = true))
    assert( ! fetch(hasTargets = true, endangered = true, travelsFar = true, reserved = true))
    assert(fetch(hasTargets = true, wantsPickup = true))
    assert(fetch(hasTargets = true, scarabTargeted = true))
  }

  test("an idle hull is fetched on any backup claim") {
    assert( ! fetch())
    assert(fetch(endangered = true))
    assert(fetch(travelsFar = true))
    assert(fetch(pairIdle = true))
    assert(fetch(strandedFetch = true))
    assert(fetch(reserved = true))
  }
}
