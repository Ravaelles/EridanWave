package starengine

import Micro.Targeting.FiltersRequired.TargetFilterFocus
import ProxyBwapi.Races.{Protoss, Zerg}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Eight Zealots against two Sunken Colonies, real UnitType numbers:
 * the line walks in and kills both statics. Managers and behaviors
 * under analysis: the shared attack-move (chase nearest valid, hold
 * max reach only with a range advantage - Zealots never have one, so
 * they always close), the committed chase target in the gun resolution
 * (mirrors toAttack: shoot the pick while it holds), and the order
 * ledger (a clean engage flips orders a handful of times, never
 * hundreds - dithering would show up here).
 */
class ZealotSunkensSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Zealot.name + StarEngineTags.Static.name + StarEngineTags.PvZ.name) }

  private def vetoed(f: Body): Boolean =
    f.kind.isBuilding && ! TargetFilterFocus.allowsBuilding(
      isBuilding = f.kind.isBuilding,
      isStaticDefense = f.kind.isStaticDefense,
      isSuicideMission = false)

  test("eight zealots clear two sunkens without anything weird") {
    val sunkens = Seq(
      new Body(Zerg.SunkenColony, -150.0, 0.0, Zerg.SunkenColony.maxTotalHealth),
      new Body(Zerg.SunkenColony, 150.0, 0.0, Zerg.SunkenColony.maxTotalHealth))
    val zealots = (0 until 8).map(i =>
      new Body(Protoss.Zealot, -280.0 + i * 80.0, 500.0, Protoss.Zealot.maxTotalHealth))
    val pilots: Map[Body, Pilot] =
      zealots.map(_ -> new Pilots.AttackMove(_.foes, (me, f) => ! vetoed(f), (0.0, 0.0))).toMap ++
        sunkens.map(_ -> new Pilots.Hold).toMap
    val world = new World(zealots, sunkens, pilots, Seq.empty)

    println(s"DIAG zealot speed=${zealots.head.speed} range=${zealots.head.rangeAgainst(sunkens.head)} " +
      s"dmg=${zealots.head.damageAgainst(sunkens.head)} cd=${zealots.head.cooldownAgainst(sunkens.head)} " +
      s"sunken range=${sunkens.head.rangeAgainst(zealots.head)} dmg=${sunkens.head.damageAgainst(zealots.head)} " +
      s"zealot hp=${Protoss.Zealot.maxTotalHealth} sunken hp=${Zerg.SunkenColony.maxTotalHealth}")
    var firstDead: Option[Int] = None
    var secondDead: Option[Int] = None
    while (world.frame < 6000 && sunkens.exists(_.alive) && zealots.exists(_.alive)) {
      world.step()
      if (firstDead.isEmpty && sunkens.count(! _.alive) >= 1) firstDead = Some(world.frame)
      if (secondDead.isEmpty && sunkens.forall(! _.alive)) secondDead = Some(world.frame)
    }

    val reissues = zealots.map(z => world.reissues.getOrElse(z, 0)).sum
    println(s"DIAG first=$firstDead second=$secondDead zealotsLeft=${zealots.count(_.alive)} reissues=$reissues")
    assert(sunkens.forall(! _.alive), "sunkens survived eight zealots")
    // Focused sunkens trade about three zealots for the pair; more than
    // that means the line is feeding instead of fighting.
    assert(zealots.count(_.alive) >= 5, "line lost more than three zealots trading two sunkens")
    assert(reissues <= 24, f"order ledger shows dithering: $reissues flips")
  }
}
