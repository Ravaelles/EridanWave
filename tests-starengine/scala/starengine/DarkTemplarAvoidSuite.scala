package starengine

import Mathematics.Points.Pixel
import Micro.Actions.Protoss.DarkTemplar.{DarkTemplarBravery, DarkTemplarRaid, DarkTemplarStaticPush, DarkTemplarStaticStandoff}
import Micro.Targeting.DarkTemplarTargets
import ProxyBwapi.Races.{Protoss, Terran}
import ProxyBwapi.UnitClasses.UnitClass
import ProxyBwapi.UnitClasses.UnitClass
import org.scalatest.funsuite.AnyFunSuite

/**
 * DT cannon avoid: a Templar raiding past a remembered Cannon must arc
 * around it unless the duel is clearly positive - walking the push at a
 * guarded gun trades the hull for nothing (ladder bill: two Templars
 * feeding one guarded Cannon). The pilot mirrors the shipped raid order
 * verbatim (avoid, push, standoff): the avoid owns the band on a negative
 * duel and releases past it, the push still chews the free lone gun.
 * Speeds, reaches, damage and sights are the real UnitType numbers; the
 * engine owns the loop only. Detectors burn through cloak by range, like
 * the game.
 */
class DarkTemplarAvoidSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.DT.name + StarEngineTags.Detector.name) }

  private def pixelOf(b: Body): Pixel = new Pixel(b.x.toInt, b.y.toInt)

  private def dpsOf(kind: UnitClass): Double =
    kind.effectiveGroundDamage.toDouble / kind.groundDamageCooldown.max(1)

  private class AvoidPilot(
    id: Int,
    goal: (Double, Double),
    remembered: Seq[Body]) extends Pilot {

    var avoidFires: Int = 0
    var pushMoves: Int = 0
    var minGunDist: Double = Double.PositiveInfinity
    private var avoiding = false

    override def direct(me: Body, world: World): Directive = {
      val guns = remembered.filter(_.alive)
      if (guns.nonEmpty) {
        val gun = guns.minBy(me.dist)
        val dist = me.dist(gun)
        minGunDist = Math.min(minGunDist, dist)
        val danger = DarkTemplarStaticStandoff.dangerTiles(
          gun.kind.pixelRangeMax.toDouble / 32.0,
          gun.kind.sightPixels.toDouble / 32.0)
        val close = dist / 32.0 <= danger + DarkTemplarStaticStandoff.closeMarginTiles
        val release = dist / 32.0 > DarkTemplarRaid.releaseDistTiles(danger)
        if (close) avoiding = true
        if (release) avoiding = false
        if (avoiding && close) {
          val pack = world.foes.view.filter(f =>
            f.alive && gun.dist(f) / 32.0 <= DarkTemplarBravery.packTiles).toSeq
          val ourDps = dpsOf(me.kind)
          val revealed = world.detectorsRevealing(me).nonEmpty
          val foeDps = if (revealed) pack.map(f => dpsOf(f.kind)).sum else 0.0
          val foeHp = pack.map(_.hp).sum
          if ( ! DarkTemplarBravery.shouldDuel(me.hp, ourDps, foeHp, foeDps)) {
            avoidFires += 1
            val out = DarkTemplarRaid.escapeFrom(pixelOf(me), pixelOf(gun), danger)

            return MoveTo(out.x.toDouble, out.y.toDouble)
          }
          avoiding = false
        }
        if (avoiding) {
          val out = DarkTemplarRaid.escapeFrom(pixelOf(me), pixelOf(gun), danger)

          return MoveTo(out.x.toDouble, out.y.toDouble)
        }
        // Unseen push, stripped to the raid: no burn, no shots landed.
        if (world.detectorsRevealing(me).isEmpty && ! world.underAttack(me)) {
          val near = world.foes.filter(f =>
            f.alive && me.dist(f) <= me.kind.sightPixels.toDouble && me.canHit(f)
              && me.dist(f) <= me.rangeAgainst(f) + DarkTemplarStaticPush.attackMarginTiles * 32.0)
          near.sortBy(me.dist).headOption match {
            case Some(prey) => return Chase(prey)
            case None =>
              pushMoves += 1

              return MoveTo(gun.x, gun.y)
          }
        }
      }

      MoveTo(goal._1, goal._2)
    }
  }

  private case class Outcome(alive: Boolean, hpEnd: Double, avoidFires: Int, pushMoves: Int, minGunDist: Double)

  private def runRaid(dtStart: (Double, Double), guards: Boolean, observer: Boolean, gunKind: UnitClass): Outcome = {
    val gun = new Body(gunKind, 0.0, 0.0, gunKind.maxTotalHealth)
    val goons = if (guards) Seq(
      new Body(Protoss.Dragoon, -100.0, 120.0, Protoss.Dragoon.maxTotalHealth),
      new Body(Protoss.Dragoon, 100.0, 120.0, Protoss.Dragoon.maxTotalHealth)) else Seq.empty
    val eyes = if (observer) Seq(new Body(Protoss.Observer, 250.0, -150.0, Protoss.Observer.maxTotalHealth)) else Seq.empty
    val dt = new Body(Protoss.DarkTemplar, dtStart._1, dtStart._2, Protoss.DarkTemplar.maxTotalHealth)
    dt.cloaked = true
    val pilot = new AvoidPilot(21, (0.0, 600.0), Seq(gun))
    val pilots: Map[Body, Pilot] = Map(dt -> pilot, gun -> new Pilots.Hold) ++
      goons.map(_ -> new Pilots.Hold) ++ eyes.map(_ -> new Pilots.Hold)
    val world = new World(Seq(dt), gun +: goons ++: eyes, pilots, Seq.empty)
    var f = 0
    while (f < 1500 && dt.alive) { world.step(); f += 1 }
    Outcome(dt.alive, dt.hp, pilot.avoidFires, pilot.pushMoves, pilot.minGunDist)
  }

  test("a revealed Templar arcs around the guarded gun instead of diving it") {
    val cannonRange = Protoss.PhotonCannon.pixelRangeMax.toDouble
    val guarded = runRaid(dtStart = (0.0, -448.0), guards = true, observer = true, gunKind = Protoss.PhotonCannon)
    println(s"DIAG guarded alive=${guarded.alive} hp=${guarded.hpEnd.toInt} avoid=${guarded.avoidFires} push=${guarded.pushMoves} minGun=${guarded.minGunDist.toInt}px")
    assert(guarded.avoidFires > 0, "the avoid never owned the band against a guarded gun")
    assert(guarded.minGunDist > DarkTemplarStaticStandoff.neverCloserTiles * 32.0,
      "the Templar closed past the 7.4-tile Cannon floor")
    assert(guarded.minGunDist > cannonRange, "the Templar entered gun reach it must never touch")
    assert(guarded.alive, "the Templar died walking past the gun")
    assert(guarded.hpEnd == Protoss.DarkTemplar.maxTotalHealth, "the Templar took fire it should never have met")
  }

  test("a lone blind gun is still chewed, never avoided") {
    val free = runRaid(dtStart = (0.0, -448.0), guards = false, observer = false, gunKind = Terran.Bunker)
    println(s"DIAG free alive=${free.alive} hp=${free.hpEnd.toInt} avoid=${free.avoidFires} push=${free.pushMoves}")
    assert(free.avoidFires == 0, "the avoid owned a blind gun the duel already wins")
    assert(free.pushMoves > 0, "the push never walked the unseen approach at the gun")
  }
}
