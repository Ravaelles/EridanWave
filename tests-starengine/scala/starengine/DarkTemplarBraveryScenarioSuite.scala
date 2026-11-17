package starengine

import Micro.Actions.Protoss.DarkTemplar.DarkTemplarBravery
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Duel bravery: no duel without a local edge. The live bill for the old
 * brain is six Templars dead 1-7 HP unconnected hunting alone - a lone
 * hull walking into a pack that outguns it. The fixed brain runs the
 * hp-weighted power comparison per pick (own hull plus nearby friends
 * vs the local guns); an unseen Templar answers everything, a revealed
 * one only the duels it outpowers. Speeds, reaches, damage and cooldowns
 * are the real UnitType numbers; both pilots call the shipped kernel,
 * the engine owns the loop only.
 *
 * Three duels, one pilot shape. A lone Templar sees three guarding
 * Dragoons under their eye: the old brain walks in and dies
 * unconnected, the fixed brain holds out of gun range untouched. The
 * same brain duels a lone unseen Dragoon to the death untouched, and a
 * wounded revealed pair it outpowers. Knowledge is modeled, not
 * omniscient: seen means inside our own sight, revealed is the world's
 * ground truth (mirrors tile.enemyDetected).
 */
class DarkTemplarBraveryScenarioSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.DT.name + StarEngineTags.Hunt.name) }

  private class DTDuelist(useBravery: Boolean) extends Pilot {
    var minGunDist: Double = Double.PositiveInfinity
    // Committed pick, mirroring toAttack: bravery gates acquisition, never
    // the ongoing duel - re-checking a bleeding hull every frame would
    // revoke the duel mid-swing and hold under fire.
    var committed: Option[Body] = None

    override def direct(me: Body, world: World): Directive = {
      val dtSight = me.kind.sightPixels.toDouble
      val seen = world.foes.filter(f => f.alive && f.kind != Protoss.Observer && me.dist(f) <= dtSight)
      if (seen.nonEmpty) minGunDist = Math.min(minGunDist, seen.map(me.dist).min)

      committed = committed.filter(_.alive)
      val pick = committed.orElse(seen.sortBy(me.dist).headOption.filter { target =>
        ! useBravery || duelWorthIt(me, target, seen, world)
      })
      committed = pick
      pick match {
        case Some(prey) => Chase(prey)
        case None       => Hold
      }
    }

    private def duelWorthIt(me: Body, pick: Body, seen: Seq[Body], world: World): Boolean = {
      val pack = seen.filter(g => pick.dist(g) <= DarkTemplarBravery.packTiles * 32.0)
      val ourDps = dpsVs(me, pick)
      val revealed = world.detectorsRevealing(me).nonEmpty
      val foeDps = if (me.cloaked && ! revealed) 0.0 else pack.map(g => dpsVs(g, me)).sum
      DarkTemplarBravery.shouldDuel(me.hp, ourDps, pack.map(_.hp).sum, foeDps)
    }

    private def dpsVs(shooter: Body, victim: Body): Double =
      shooter.damageAgainst(victim).toDouble / shooter.cooldownAgainst(victim).max(1)
  }

  private case class Outcome(dtAlive: Boolean, dtHp: Double, minGunDist: Double, foesAlive: Int)

  private def runDuel(
    useBravery: Boolean,
    guards: Seq[(Double, Double, Double)],
    withEye: Boolean,
    frames: Int): Outcome = {
    val dt = new Body(Protoss.DarkTemplar, 0.0, -200.0, Protoss.DarkTemplar.maxTotalHealth)
    dt.cloaked = true
    val foes = guards.map { case (x, y, hp) =>
      new Body(Protoss.Dragoon, x, y, hp)
    } ++ (if (withEye) Seq(new Body(Protoss.Observer, 0.0, 50.0, Protoss.Observer.maxTotalHealth)) else Seq.empty)
    val pilot = new DTDuelist(useBravery)
    val pilots: Map[Body, Pilot] = Map(dt -> pilot) ++ foes.map(_ -> new Pilots.Hold)
    val world = new World(Seq(dt), foes, pilots, Seq.empty)
    var f = 0
    while (f < frames && dt.alive) { world.step(); f += 1 }
    Outcome(dt.alive, dt.hp, pilot.minGunDist, foes.count(f => f.alive && f.kind != Protoss.Observer))
  }

  test("a lone Templar refuses a pack that outguns it") {
    val full = Protoss.Dragoon.maxTotalHealth.toDouble
    val dtDps = Protoss.DarkTemplar.groundDamageCooldown
    println(s"DIAG dtCd=$dtDps goonDps=${Protoss.Dragoon.effectiveGroundDamage}/${Protoss.Dragoon.groundDamageCooldown}")
    val fixed = runDuel(
      useBravery = true,
      guards = Seq((-60.0, 0.0, full), (0.0, 0.0, full), (60.0, 0.0, full)),
      withEye = true, frames = 400)
    println(s"DIAG fixed alive=${fixed.dtAlive} hp=${fixed.dtHp.toInt} minGun=${fixed.minGunDist.toInt}px foesAlive=${fixed.foesAlive}")
    assert(fixed.dtAlive, "the Templar died holding - the refusal never fired")
    assert(fixed.dtHp == Protoss.DarkTemplar.maxTotalHealth, "the Templar took fire it refused to meet")
    assert(fixed.minGunDist > 150.0, "the Templar closed a pack it cannot outpower")

    val old = runDuel(
      useBravery = false,
      guards = Seq((-60.0, 0.0, full), (0.0, 0.0, full), (60.0, 0.0, full)),
      withEye = true, frames = 400)
    println(s"DIAG old alive=${old.dtAlive} hp=${old.dtHp.toInt} foesAlive=${old.foesAlive}")
    assert( ! old.dtAlive, "the control survived the walk-in - nothing to fix here")
  }

  test("the same brain duels a lone unseen Dragoon to the death untouched") {
    val full = Protoss.Dragoon.maxTotalHealth.toDouble
    val fixed = runDuel(
      useBravery = true,
      guards = Seq((0.0, 0.0, full)),
      withEye = false, frames = 600)
    println(s"DIAG duel alive=${fixed.dtAlive} hp=${fixed.dtHp.toInt} foesAlive=${fixed.foesAlive}")
    assert(fixed.foesAlive == 0, "the Templar refused a free kill - bravery pacifies winning duels")
    assert(fixed.dtAlive && fixed.dtHp == Protoss.DarkTemplar.maxTotalHealth, "a free kill cost hull")
  }

  test("wounds move the same comparison: a beaten pair is dueled") {
    val fixed = runDuel(
      useBravery = true,
      guards = Seq((-40.0, 0.0, 40.0), (40.0, 0.0, 40.0)),
      withEye = true, frames = 600)
    println(s"DIAG wounded alive=${fixed.dtAlive} hp=${fixed.dtHp.toInt} foesAlive=${fixed.foesAlive}")
    assert(fixed.foesAlive == 0, "the Templar held a pack it outpowers - wounds must move the sums")
    assert(fixed.dtAlive, "the winning duel still killed the Templar")
  }
}
