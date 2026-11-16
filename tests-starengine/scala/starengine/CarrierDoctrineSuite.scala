package starengine

import Micro.Targeting.CarrierTargets
import ProxyBwapi.Races.{Protoss, Terran}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Carrier doctrine, step by step: a Carrier faces a Missile Turret
 * and a Marine, both inside its gun. The pilot calls the shipped
 * CarrierTargets ladder on marks built from the live bodies and
 * commits to the pick: AA first, unconditionally, nearest inside
 * the tier. The turret must die before the marine takes a scratch.
 * Speeds, reaches and damage are the real UnitType numbers.
 */
class CarrierDoctrineSuite extends AnyFunSuite with StarEngineSuite {

  private def markOf(me: Body, foe: Body): CarrierTargets.Mark =
    CarrierTargets.Mark(
      me.dist(foe) / 32.0,
      foe.kind == Terran.MissileTurret,
      foe.kind == Terran.Goliath,
      foe.kind == Terran.SiegeTankSieged,
      foe.kind == Terran.SiegeTankUnsieged,
      foe.kind == Terran.Marine,
      foe.kind == Terran.Bunker,
      foe.kind == Terran.Medic,
      isBuilderWorker = false,
      foe.kind.isWorker)

  private class DoctrinePilot extends Pilot {

    override def direct(me: Body, world: World): Directive = {
      val foes = world.foes.filter(_.alive)
      val marks = foes.map(markOf(me, _))
      CarrierTargets.pick(marks).map(i => Chase(foes(i))).getOrElse(Hold)
    }
  }

  test("AA first: the turret takes fire while the marine stands untouched") {
    val carrier = new Body(Protoss.Carrier, 0.0, 500.0, Protoss.Carrier.maxTotalHealth)
    val turret = new Body(Terran.MissileTurret, 200.0, 500.0, Terran.MissileTurret.maxTotalHealth)
    val marine = new Body(Terran.Marine, 250.0, 520.0, Terran.Marine.maxTotalHealth)
    val pilot = new DoctrinePilot
    val world = new World(Seq(carrier), Seq(turret, marine),
      Map(carrier -> pilot, turret -> new Pilots.Hold, marine -> new Pilots.Hold), Seq.empty)
    val marineHp0 = marine.hp
    val turretHp0 = turret.hp

    while (world.frame < 300 && carrier.alive) {
      world.step()
    }

    println(s"DIAG doctrine frames=${world.frame} turret=${turret.hp.toInt}/$turretHp0 marine=${marine.hp.toInt}/$marineHp0 carrier=${carrier.hp.toInt}")
    assert(carrier.alive, "carrier died before the doctrine proved anything")
    assert(turret.hp < turretHp0, "carrier never fired on the turret")
    assert(marine.hp == marineHp0, "marine took fire while the turret stood")
  }
}
