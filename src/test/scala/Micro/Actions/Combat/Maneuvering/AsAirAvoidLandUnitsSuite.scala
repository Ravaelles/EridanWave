package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class AsAirAvoidLandUnitsSuite extends AnyFunSuite {

  private val mapW = 4096
  private val mapH = 4096

  test("nothing in trigger means no flee") {
    assert(AsAirAvoidLandUnits.fleePixel(Pixel(0, 0), Seq(Pixel(13 * 32, 0)), wound01 = 1.0, mapW, mapH).isEmpty)
    assert(AsAirAvoidLandUnits.fleePixel(Pixel(0, 0), Seq.empty, wound01 = 1.0, mapW, mapH).isEmpty)
  }

  test("a threat on the edge still triggers") {
    assert(AsAirAvoidLandUnits.fleePixel(Pixel(2000, 2000), Seq(Pixel(2000 + 12 * 32, 2000)), wound01 = 0.0, mapW, mapH).isDefined)
  }

  test("one threat flees straight away from it") {
    val away = AsAirAvoidLandUnits.fleePixel(Pixel(2000, 2320), Seq(Pixel(2000, 2000)), wound01 = 0.0, mapW, mapH)
    assert(away.exists(p => p.y > 2320 && p.x == 2000))
  }

  test("dying hulls keep the full ten tiles") {
    val fresh = AsAirAvoidLandUnits.fleePixel(Pixel(2000, 2320), Seq(Pixel(2000, 2000)), wound01 = 0.0, mapW, mapH)
    val dying = AsAirAvoidLandUnits.fleePixel(Pixel(2000, 2320), Seq(Pixel(2000, 2000)), wound01 = 1.0, mapW, mapH)
    assert(fresh.isDefined && dying.isDefined)
    assert(dying.get.y - 2320 > fresh.get.y - 2320)
  }

  test("many threats flee their centroid") {
    val away = AsAirAvoidLandUnits.fleePixel(Pixel(2160, 2320), Seq(Pixel(2000, 2000), Pixel(2320, 2000)), wound01 = 0.0, mapW, mapH)
    assert(away.exists(p => p.y > 2320 && p.x == 2160))
  }

  test("a corner destination slides out instead of diving in") {
    val out = AsAirAvoidLandUnits.outOfCorner(Pixel(10, 10), mapW, mapH)
    assert(out.x >= 192 || out.y >= 192)
    val other = AsAirAvoidLandUnits.outOfCorner(Pixel(4086, 10), mapW, mapH)
    assert(other.x <= mapW - 192 || other.y >= 192)
    val bottom = AsAirAvoidLandUnits.outOfCorner(Pixel(10, 4086), mapW, mapH)
    assert(bottom.x >= 192 || bottom.y <= mapH - 192)
  }

  test("edges and center pass through untouched") {
    assert(AsAirAvoidLandUnits.outOfCorner(Pixel(2000, 10), mapW, mapH) == Pixel(2000, 10))
    assert(AsAirAvoidLandUnits.outOfCorner(Pixel(2000, 2000), mapW, mapH) == Pixel(2000, 2000))
  }

  test("an overshooting flee clamps to the map") {
    val out = AsAirAvoidLandUnits.outOfCorner(Pixel(-50, 2000), mapW, mapH)
    assert(out == Pixel(0, 2000))
  }
}
