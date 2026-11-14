package Micro.Actions.Combat.Maneuvering

import Mathematics.Points.Pixel
import org.scalatest.funsuite.AnyFunSuite

class RangedAvoidanceSuite extends AnyFunSuite {

  private val avoidance = new RangedAvoidance()

  test("nothing in trigger means no flee") {
    assert(avoidance.fleePixel(Pixel(0, 0), Seq(Pixel(13 * 32, 0)), wound01 = 1.0).isEmpty)
    assert(avoidance.fleePixel(Pixel(0, 0), Seq.empty, wound01 = 1.0).isEmpty)
  }

  test("a threat on the edge still triggers") {
    assert(avoidance.fleePixel(Pixel(0, 0), Seq(Pixel(12 * 32, 0)), wound01 = 0.0).isDefined)
  }

  test("one threat flees straight away from it") {
    val away = avoidance.fleePixel(Pixel(320, 320), Seq(Pixel(320, 0)), wound01 = 0.0)
    assert(away.exists(p => p.y > 320 && p.x == 320))
  }

  test("dying hulls keep the full ten tiles") {
    val fresh = avoidance.fleePixel(Pixel(320, 320), Seq(Pixel(320, 0)), wound01 = 0.0)
    val dying = avoidance.fleePixel(Pixel(320, 320), Seq(Pixel(320, 0)), wound01 = 1.0)
    assert(fresh.isDefined && dying.isDefined)
    assert(dying.get.y - 320 > fresh.get.y - 320)
  }

  test("many threats flee their centroid") {
    val away = avoidance.fleePixel(Pixel(160, 320), Seq(Pixel(0, 0), Pixel(320, 0)), wound01 = 0.0)
    assert(away.exists(p => p.y > 320 && p.x == 160))
  }
}
