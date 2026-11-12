package Planning

import org.scalatest.funsuite.AnyFunSuite

class YoloSuite extends AnyFunSuite {

  test("short supply never frenzies, however deep the bank") {
    assert( ! Yolo.frenzyTrips(194, 5000, 5000))
    assert( ! Yolo.frenzyTrips(0, 1500, 250))
  }

  test("a thin bank never frenzies at max supply") {
    assert( ! Yolo.frenzyTrips(195, 999, 1000))
    assert( ! Yolo.frenzyTrips(200, 1000, 249))
    assert( ! Yolo.frenzyTrips(200, 1499, 0))
  }

  test("minerals plus gas trip the frenzy at max supply") {
    assert(Yolo.frenzyTrips(195, 1000, 250))
    assert(Yolo.frenzyTrips(200, 3000, 1000))
  }

  test("a mineral mountain alone trips the frenzy") {
    assert(Yolo.frenzyTrips(195, 1500, 0))
    assert(Yolo.frenzyTrips(200, 5000, 100))
  }

  test("the frenzy holds until supply drops below 170") {
    assert( ! Yolo.frenzyBreaks(170))
    assert( ! Yolo.frenzyBreaks(195))
    assert(Yolo.frenzyBreaks(169))
    assert(Yolo.frenzyBreaks(0))
  }
}
