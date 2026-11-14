package starengine

import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Zealot mirror from a real map snapshot (ums/rav/protoss/3Zeal_v_3Zeal):
 * three of ours against three of theirs, starting positions dumped
 * from a live game (ERIDAN_DUMP_SNAPSHOT=1) instead of hand-placed.
 * The snapshot had an observer on our side; it is omitted on purpose -
 * this engine has no detection mechanics by design, and it never fights.
 * The live map flips between VICTORY and DEFEAT run to run, so this
 * pins the mechanism (the fight happens, no dithering), not the score.
 */
class ZealotMirrorSuite extends AnyFunSuite {

  test("a mirrored zealot line resolves without dithering") {
    val ours = Seq(
      new Body(Protoss.Zealot, 1956.0, 1327.0, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Zealot, 1885.0, 1225.0, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Zealot, 1894.0, 1419.0, Protoss.Zealot.maxTotalHealth))
    val foes = Seq(
      new Body(Protoss.Zealot, 2171.0, 1469.0, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Zealot, 2185.0, 1528.0, Protoss.Zealot.maxTotalHealth),
      new Body(Protoss.Zealot, 2194.0, 1314.0, Protoss.Zealot.maxTotalHealth))
    val pilots: Map[Body, Pilot] =
      (ours ++ foes).map(b => b -> new Pilots.AttackMove(
        if (ours.contains(b)) _.foes else _.ours,
        (_, _) => true,
        (2000.0, 1400.0))).toMap
    val world = new World(ours, foes, pilots, Seq.empty)

    while (world.frame < 3000 && foes.exists(_.alive) && ours.exists(_.alive)) {
      world.step()
    }
    val foeDeaths = foes.count(! _.alive)
    val ourDeaths = ours.count(! _.alive)

    val reissues = (ours ++ foes).map(b => world.reissues.getOrElse(b, 0)).sum
    println(s"DIAG oursAlive=${ours.count(_.alive)} foesAlive=${foes.count(_.alive)} reissues=$reissues frames=${world.frame}")
    assert(world.frame < 3000, "mirror never resolved")
    assert(foeDeaths + ourDeaths > 0, "nobody fought")
    assert(reissues <= 24, f"mirror dithers: $reissues order changes")
  }
}
