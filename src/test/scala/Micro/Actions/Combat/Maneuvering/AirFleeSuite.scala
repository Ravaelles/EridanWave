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

  test("a custom trigger reaches past the shared twelve") {
    val at13 = Seq(Pixel(13 * 32, 0))
    assert(AirFlee.fleePixel(Pixel(0, 0), at13, wound01 = 0.0, mapW, mapH).isEmpty)
    assert(AirFlee.fleePixel(
      Pixel(0, 0), at13, wound01 = 0.0, mapW, mapH,
      AirFlee.marginLowTiles, AirFlee.marginHighTiles, triggerTiles = 14.0).isDefined)
    assert(AirFlee.fleePixel(
      Pixel(0, 0), Seq(Pixel(15 * 32, 0)), wound01 = 0.0, mapW, mapH,
      AirFlee.marginLowTiles, AirFlee.marginHighTiles, triggerTiles = 14.0).isEmpty)
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
}
