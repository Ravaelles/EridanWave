package Micro.Actions.Protoss.Shuttle

import Micro.Actions.Protoss.Shuttle.ReaverDropSim._
import org.scalatest.funsuite.AnyFunSuite

class ReaverDropSimSuite extends AnyFunSuite {

  private val p = SimParams.production()

  test("production params mirror the engine") {
    // Shuttle outruns Hydras, Reavers crawl, Scarabs scream.
    assert(p.shuttleSpeedPx > p.hydraSpeedPx, "shuttle outruns hydras " + p.shuttleSpeedPx + " vs " + p.hydraSpeedPx)
    assert(p.hydraSpeedPx > p.reaverSpeedPx, "hydras outrun reavers")
    assert(p.scarabSpeedPx > p.shuttleSpeedPx, "scarabs outrun shuttles")
    // Reaver outranges Hydras; Hydras reach four tiles.
    assert(p.reaverRangePx == 8 * 32.0, "reaver reach " + p.reaverRangePx)
    assert(p.hydraRangePx > 3 * 32.0 && p.hydraRangePx <= 5 * 32.0, "hydra reach " + p.hydraRangePx)
    // One Scarab kills one Hydra.
    assert(p.scarabDmg >= p.hydraHp, "scarab one-shots hydras")
    // Preemptive band matches production: 4 tiles fresh, up to 9 dying.
    assert(p.preemptBaseTiles == 4.0 && p.preemptWoundTiles == 9.0)
  }

  test("the landing zone is the base corner farthest from the pack") {
    val base = P(0, 0)
    val hydras = Seq(P(0, 400), P(100, 420), P(-100, 420), P(0, 500))
    val drop = chooseDropPoint(base, halfTiles = 6.0, hydras.map(h => h), shuttle = P(0, 1500))
    // Hydras hold the south: the drop belongs north of the base.
    assert(drop.y < 0, "drop went south into the pack: " + drop)
  }

  private def flankWorld(): World = {
    val base = P(0, 0)
    val hydraPos = Seq(P(0, 400), P(100, 420), P(-100, 420), P(0, 500))
    val drop = chooseDropPoint(base, 6.0, hydraPos, P(0, 1500))
    new World(
      p = p,
      base = base,
      baseHalfTiles = 6.0,
      shuttle = P(0, 1500),
      shuttleHp = (p.shuttleHp, p.shuttleShields),
      reaver = P(0, 1500),
      reaverHp = (p.reaverHp, p.reaverShields),
      loaded = true,
      dropPoint = drop,
      hydras = hydraPos.map(pos => new Hydra(pos, p.hydraHp)))
  }

  test("a flank drop lands, shoots and takes no hits") {
    val w = flankWorld()
    w.run(8000)
    assert(w.unloads >= 1, "never unloaded in " + w.frame + " frames")
    assert(w.scarabsFired >= 1, "never fired, hydras alive: " + w.hydras.count(_.hp > 0))
    assert(w.hits.isEmpty, "took hits: " + w.hits.take(5).mkString(", "))
    assert(w.hydras.forall(_.hp <= 0), "hydras survived: " + w.hydras.count(_.hp > 0))
  }

  test("the shuttle does not dither: few hard direction flips") {
    val w = flankWorld()
    w.run(8000)
    assert(w.reversals < 60, "dithering: " + w.reversals + " hard flips")
  }

  test("a parked Reaver amid Hydras takes hits (sim bites)") {
    val w = flankWorld()
    // Sabotage the doctrine: unload in the middle of the pack, never move.
    w.dropPoint = P(0, 430)
    w.shuttle = P(0, 430)
    w.reaver = P(0, 430)
    w.loaded = false
    w.unloads = 1
    w.run(300)
    assert(w.hits.nonEmpty, "sim cannot detect hits")
    assert(w.reaverHp._1 < p.reaverHp || w.reaverHp._2 < p.reaverShields, "reaver untouched amid hydras")
  }
}
