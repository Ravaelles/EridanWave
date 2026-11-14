package starengine

import Micro.Targeting.FiltersRequired.TargetFilterFocus
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Reaver against zombie pressure (no statics, no leash): hungry
 * Zerglings stream straight at the pair and chew the nearest
 * most-wounded body. The Reaver's AttackMove clears the escort the
 * same way it does under a Sunken - nearest valid foe first - while
 * the ride holds untouched behind it. Small pack: the Reaver eats it
 * alone. Large pack: the pair needs Dragoon escorts or the teeth win.
 */
class ReaverZombieSuite extends AnyFunSuite {

  private def vetoed(f: Body): Boolean =
    f.kind.isBuilding && ! TargetFilterFocus.allowsBuilding(
      isBuilding = f.kind.isBuilding,
      isStaticDefense = f.kind.isStaticDefense,
      isSuicideMission = false)

  private def lings(n: Int, cx: Double, cy: Double, gap: Double = 32.0): Seq[Body] =
    (0 until n).map(i =>
      new Body(Zerg.Zergling, cx + (i % 3) * gap - gap, cy + (i / 3) * gap, Zerg.Zergling.maxTotalHealth))

  private def pair(reaverPos: (Double, Double)): (Body, Body) =
    (new Body(Protoss.Reaver, reaverPos._1, reaverPos._2, Protoss.Reaver.maxTotalHealth),
      new Body(Protoss.Shuttle, reaverPos._1, reaverPos._2, Protoss.Shuttle.maxTotalHealth))

  test("a reaver eats a small zombie pack alone, the ride untouched") {
    val (reaver, shuttle) = pair((0.0, 600.0))
    val pack = lings(2, 0.0, 200.0)
    val pilots: Map[Body, Pilot] = Map(
      reaver -> new Pilots.AttackMove(_.foes, (me, f) => ! vetoed(f), (0.0, 0.0)),
      shuttle -> new Pilots.Hold) ++
      pack.map(l => l -> new Pilots.ZombieAttack(_.ours, (0.0, 600.0))).toMap
    val world = new World(Seq(reaver, shuttle), pack, pilots, Seq.empty)

    while (world.frame < 3000 && pack.exists(_.alive) && reaver.alive) {
      world.step()
    }

    println(s"DIAG reaver hp=${reaver.hp.toInt}/${reaver.maxHp.toInt} shuttle=${shuttle.hp.toInt} frames=${world.frame}")
    assert(pack.forall(! _.alive), "reaver never cleared two zombie lings")
    assert(reaver.alive, "reaver died to two lings")
    assert(shuttle.hp == shuttle.maxHp, "shuttle took hits holding behind the reaver")
  }

  test("a large zombie pack needs dragoon escorts, the pair alone is teeth food") {
    val (reaver, shuttle) = pair((0.0, 600.0))
    // Spread past splash overlap: twelve lings converge faster than one
    // scarab per minute clears them, so the lone hull dies chewing.
    val pack = lings(16, 0.0, 200.0, gap = 150.0)
    val pilots: Map[Body, Pilot] = Map(
      reaver -> new Pilots.AttackMove(_.foes, (me, f) => ! vetoed(f), (0.0, 0.0)),
      shuttle -> new Pilots.Hold) ++
      pack.map(l => l -> new Pilots.ZombieAttack(_.ours, (0.0, 600.0))).toMap
    val world = new World(Seq(reaver, shuttle), pack, pilots, Seq.empty)

    while (world.frame < 3000 && pack.exists(_.alive) && reaver.alive) {
      world.step()
    }

    println(s"DIAG lone reaver alive=${reaver.alive} lingsLeft=${pack.count(_.alive)} frames=${world.frame}")
    assert( ! reaver.alive, "lone reaver should not survive sixteen hungry lings - the escort rule exists for this")
  }

  test("dragoon escorts carry the pair through the large pack") {
    val (reaver, shuttle) = pair((0.0, 600.0))
    val dragoons = Seq(
      new Body(Protoss.Dragoon, -80.0, 620.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 80.0, 620.0, Protoss.Dragoon.maxTotalHealth))
    val pack = lings(6, 0.0, 200.0)
    val ours = Seq(reaver, shuttle) ++ dragoons
    val pilots: Map[Body, Pilot] = Map(
      reaver -> new Pilots.AttackMove(_.foes, (me, f) => ! vetoed(f), (0.0, 0.0)),
      shuttle -> new Pilots.Hold) ++
      dragoons.map(_ -> new Pilots.AttackMove(_.foes, (me, f) => me.canHit(f), (0.0, 620.0))).toMap ++
      pack.map(l => l -> new Pilots.ZombieAttack(_.ours, (0.0, 600.0))).toMap
    val world = new World(ours, pack, pilots, Seq.empty)

    while (world.frame < 4000 && pack.exists(_.alive) && (reaver.alive || dragoons.exists(_.alive))) {
      world.step()
    }

    println(s"DIAG escorted reaver alive=${reaver.alive} lingsLeft=${pack.count(_.alive)} shuttle=${shuttle.hp.toInt}/${shuttle.maxHp.toInt}")
    assert(pack.forall(! _.alive), "escorted pair never cleared six zombie lings")
    assert(reaver.alive, "reaver died under dragoon escort")
    assert(shuttle.hp == shuttle.maxHp, "shuttle took hits holding behind escorts")
  }
}
