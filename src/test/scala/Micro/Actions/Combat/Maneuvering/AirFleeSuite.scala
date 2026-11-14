package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class AirFleeSuite extends AnyFunSuite {

  private val mapW = 4096
  private val mapH = 4096

  test("nothing in trigger means no flee") {
    assert(AirFlee.fleePixel(Pixel(0, 0), Seq(Pixel(13 * 32, 0)), wound01 = 1.0, mapW, mapH).isEmpty)
    assert(AirFlee.fleePixel(Pixel(0, 0), Seq.empty, wound01 = 1.0, mapW, mapH).isEmpty)
  }

  test("a threat on the edge still triggers") {
    assert(AirFlee.fleePixel(Pixel(2000, 2000), Seq(Pixel(2000 + 12 * 32, 2000)), wound01 = 0.0, mapW, mapH).isDefined)
  }

  test("one threat flees straight away from it") {
    val away = AirFlee.fleePixel(Pixel(2000, 2320), Seq(Pixel(2000, 2000)), wound01 = 0.0, mapW, mapH)
    assert(away.exists(p => p.y > 2320 && p.x == 2000))
  }

  test("dying hulls keep the full ten tiles") {
    val fresh = AirFlee.fleePixel(Pixel(2000, 2320), Seq(Pixel(2000, 2000)), wound01 = 0.0, mapW, mapH)
    val dying = AirFlee.fleePixel(Pixel(2000, 2320), Seq(Pixel(2000, 2000)), wound01 = 1.0, mapW, mapH)
    assert(fresh.isDefined && dying.isDefined)
    assert(dying.get.y - 2320 > fresh.get.y - 2320)
  }

  test("many threats flee their centroid") {
    val away = AirFlee.fleePixel(Pixel(2160, 2320), Seq(Pixel(2000, 2000), Pixel(2320, 2000)), wound01 = 0.0, mapW, mapH)
    assert(away.exists(p => p.y > 2320 && p.x == 2160))
  }

  test("a corner destination slides out along the shallower penetration") {
    assert(AirFlee.outOfCorner(Pixel(10, 10), mapW, mapH) == Pixel(10, 192))
    assert(AirFlee.outOfCorner(Pixel(4086, 10), mapW, mapH) == Pixel(4086, 192))
    assert(AirFlee.outOfCorner(Pixel(10, 4086), mapW, mapH) == Pixel(10, mapH - 192))
  }

  test("edges and center pass through untouched") {
    assert(AirFlee.outOfCorner(Pixel(2000, 10), mapW, mapH) == Pixel(2000, 10))
    assert(AirFlee.outOfCorner(Pixel(2000, 2000), mapW, mapH) == Pixel(2000, 2000))
  }

  test("an overshooting flee clamps to the map") {
    val out = AirFlee.outOfCorner(Pixel(-50, 2000), mapW, mapH)
    assert(out == Pixel(0, 2000))
  }
}
