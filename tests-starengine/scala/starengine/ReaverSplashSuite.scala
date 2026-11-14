package starengine

import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Reaver splash approximation (real BWAPI radii, Scarab's for the
 * Reaver): the shot lands instantly - no scarab flight - the primary
 * takes it full and every other live enemy in the outer radius takes
 * half. Six Zerglings standing shoulder to shoulder die to ~one scarab
 * the way packed bio does in the game; the same six standing apart
 * cost one scarab each. Lings hold still so the geometry stays exact;
 * the Reaver outranges them either way.
 */
class ReaverSplashSuite extends AnyFunSuite {

  private def pack(spacingPx: Double): Seq[Body] =
    (0 until 6).map(i =>
      new Body(Zerg.Zergling, (i % 3) * spacingPx, 400.0 + (i / 3) * spacingPx, Zerg.Zergling.maxTotalHealth))

  private def runPack(lings: Seq[Body]): (Option[Int], Int) = {
    val reaver = new Body(Protoss.Reaver, 0.0, 800.0, Protoss.Reaver.maxTotalHealth)
    val pilots: Map[Body, Pilot] = Map(
      reaver -> new Pilots.AttackMove(_.foes, (_, _) => true, (0.0, 400.0))) ++
      lings.map(_ -> new Pilots.Hold).toMap
    val world = new World(Seq(reaver), lings, pilots, Seq.empty)
    var cleared: Option[Int] = None
    var shots = 0
    while (world.frame < 4000 && lings.exists(_.alive) && reaver.alive) {
      val loaded = reaver.cooldownLeft == 0
      world.step()
      if (loaded && reaver.cooldownLeft > 0) shots += 1
      if (cleared.isEmpty && lings.forall(! _.alive)) cleared = Some(world.frame)
    }
    println(s"DIAG cleared=$cleared shots=$shots reaver=${reaver.hp.toInt}/${reaver.maxHp.toInt}")
    (cleared, shots)
  }

  test("a packed escort dies to splash, a spread one costs scarabs") {
    val reaver = new Body(Protoss.Reaver, 0.0, 0.0, Protoss.Reaver.maxTotalHealth)
    println(s"DIAG splash=${reaver.splashOuterPx}px dmg=${reaver.damageAgainst(pack(30.0).head)} " +
      s"cd=${reaver.cooldownAgainst(pack(30.0).head)} lingHp=${Zerg.Zergling.maxTotalHealth}")
    val (packedClear, packedShots) = runPack(pack(30.0))
    val (spreadClear, spreadShots) = runPack(pack(250.0))
    println(s"DIAG packed clear=$packedClear shots=$packedShots spread clear=$spreadClear shots=$spreadShots")
    assert(packedClear.isDefined, "reaver never cleared the packed escort")
    assert(spreadClear.isDefined, "reaver never cleared the spread escort")
    assert(packedClear.get < spreadClear.get, "splash did not speed up the packed kill")
    assert(packedShots < spreadShots, "packed escort should cost fewer scarabs")
  }
}
