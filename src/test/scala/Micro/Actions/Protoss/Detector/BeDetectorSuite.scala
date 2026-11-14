package Micro.Actions.Protoss.Detector

import org.scalatest.funsuite.AnyFunSuite

class BeDetectorSuite extends AnyFunSuite {

  test("a detector inside eleven tiles with hunters inside twenty means run") {
    assert(BeDetector.shouldFleeHunters(detectorTiles = 0.0, hunterTiles = 0.0))
    assert(BeDetector.shouldFleeHunters(detectorTiles = 11.0, hunterTiles = 20.0))
  }

  test("a far detector means hold, however close the hunters") {
    assert( ! BeDetector.shouldFleeHunters(detectorTiles = 11.01, hunterTiles = 0.0))
    assert( ! BeDetector.shouldFleeHunters(detectorTiles = 200.0, hunterTiles = 0.0))
  }

  test("far hunters mean hold, however close the detector") {
    assert( ! BeDetector.shouldFleeHunters(detectorTiles = 0.0, hunterTiles = 20.01))
    assert(BeDetector.shouldFleeHunters(detectorTiles = 11.0, hunterTiles = 19.99))
  }
}
