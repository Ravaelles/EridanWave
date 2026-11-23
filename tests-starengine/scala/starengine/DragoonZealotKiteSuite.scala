package starengine

import Micro.Actions.Combat.Decisioning.Kiting.KiteAway
import ProxyBwapi.Races.Protoss
import org.scalatest.funsuite.AnyFunSuite

/**
 * Dragoon kites a lone Zealot, step by step: fresh or not, the chase
 * starts at five tiles - never after the first hit - volleys plant only
 * past contact, and a loaded hull with blades on top still steps out.
 * A kiter that runs more than six tiles from its pack steers back toward
 * it instead of across the map. Speeds, reaches, damage and cooldowns
 * are the real UnitType numbers; the pilot composes the shipped kernels
 * in shipped order, the engine owns the loop only.
 */
class DragoonZealotKiteSuite extends AnyFunSuite with StarEngineSuite {

  override def tags: Map[String, Set[String]] =
    super.tags.map { case (n, s) => n -> (s + StarEngineTags.Dragoon.name + StarEngineTags.Zealot.name + StarEngineTags.Pressure.name + StarEngineTags.PvP.name) }

  private class KitePilot(mate: Option[Body], useLeash: Boolean) extends Pilot {
    var plants = 0
    var yields = 0
    var leashes = 0
    var maxSepTiles = 0.0

    override def direct(me: Body, world: World): Directive = {
      val foe = world.foes.filter(_.alive).sortBy(me.dist).headOption.getOrElse(return Hold)
      val distTiles = me.dist(foe) / 32.0
      val rangeTiles = me.rangeAgainst(foe) / 32.0
      val gunsNear = world.foes.exists(f => f.alive && me.canHit(f) && ! f.kind.melee && me.dist(f) <= KiteAway.chaseTiles * 32.0)
      val contact = me.dist(foe) <= KiteAway.steadyContactTiles * 32.0
      // Plant rules (production order): the edge plants a loaded volley,
      // anywhere else a loaded gun with no guns around kites first and
      // shoots from the edge instead of trading contact.
      val edge = KiteAway.holdsTheEdge(distTiles, rangeTiles)
      val loaded = KiteAway.holdsForVolley(me.cooldownLeft)
      if ((edge && (gunsNear || loaded)) || (gunsNear && loaded)) {
        plants += 1

        return Hold

      }

      // Kite (production order): the walk-back inside the cooldown,
      // floored at the escape step with blades on top, capped at the edge.
      yields += 1
      val woundWant = (1.0 - me.hp / me.maxHp) * KiteAway.maxOffsetTiles * 32.0
      val walkback = math.max(0, me.cooldownLeft - 2) * me.speed
      var step = KiteAway.edgeCappedYield(
        KiteAway.contactYield(woundWant, walkback), distTiles, rangeTiles, KiteAway.kiteEdgeTiles)
      if ( ! gunsNear && contact) step = KiteAway.escapeYield(step, KiteAway.meleeEscapeTiles)
      val dx = me.x - foe.x
      val dy = me.y - foe.y
      val d = Math.sqrt(dx * dx + dy * dy)
      var tx = me.x + dx / d * step
      var ty = me.y + dy / d * step
      mate.filter(_.alive).foreach { m =>
        val sep = me.dist(m) / 32.0
        maxSepTiles = math.max(maxSepTiles, sep)
        val aligned = (m.x - me.x) * (me.x - foe.x) + (m.y - me.y) * (me.y - foe.y) > 0
        if (useLeash && ! gunsNear && KiteAway.shouldLeash(sep, KiteAway.leashTiles, m.dist(foe), me.dist(foe), aligned)) {
          leashes += 1
          val over = math.hypot(tx - m.x, ty - m.y) - KiteAway.leashTiles * 32.0
          if (over > 0) {
            val d = math.hypot(m.x - tx, m.y - ty)
            tx = tx + (m.x - tx) / d * over
            ty = ty + (m.y - ty) / d * over
          }
        }
      }
      // Committed strides mirror production: short hops brake into
      // arrival every few frames, so the yield strides past its point.
      val commitPx = KiteAway.kiteCommitTiles * 32.0
      val runLen = math.hypot(tx - me.x, ty - me.y)
      if (runLen < commitPx && runLen > 0) {
        tx = me.x + (tx - me.x) / runLen * commitPx
        ty = me.y + (ty - me.y) / runLen * commitPx
      }
      MoveTo(tx, ty)
    }
  }

  private class StandPilot extends Pilot {
    override def direct(me: Body, world: World): Directive = {
      val foe = world.foes.filter(_.alive).sortBy(me.dist).headOption.getOrElse(return Hold)

      val fresh = me.hp >= me.maxHp
      val loaded = me.cooldownLeft <= 10
      val contact = me.dist(foe) <= KiteAway.steadyContactTiles * 32.0
      // The old brain: a fresh hull stands while the blade closes, loaded
      // volleys plant even point blank, and only wounded hulls yield - and
      // then without an escape floor or a leash.
      if (fresh && ! contact) return Hold

      if (loaded) return Hold

      val dx = me.x - foe.x
      val dy = me.y - foe.y
      val d = Math.sqrt(dx * dx + dy * dy)
      if (d > 0) {
        val edge = me.rangeAgainst(foe) - KiteAway.kiteEdgeTiles * 32.0
        val step = math.min(math.max(0, me.cooldownLeft - 2) * me.speed, math.max(0.0, edge - me.dist(foe)))
        MoveTo(me.x + dx / d * step, me.y + dy / d * step)
      } else Hold
    }
  }

  private case class Outcome(
    alive: Boolean,
    hpEnd: Double,
    hitsTaken: Int,
    foeAlive: Boolean,
    plants: Int,
    yields: Int,
    leashes: Int,
    maxSepTiles: Double,
    frames: Int,
    minDistPx: Double)

  private def runDuel(useLeash: Boolean, oldBrain: Boolean, mateAt: Option[(Double, Double)], frames: Int = 3000): Outcome = {
    val dragoon = new Body(Protoss.Dragoon, 0.0, 0.0, Protoss.Dragoon.maxTotalHealth)
    val zealot = new Body(Protoss.Zealot, 0.0, -150.0, Protoss.Zealot.maxTotalHealth)
    val mate = mateAt.map { case (x, y) =>
      val m = new Body(Protoss.Dragoon, x, y, Protoss.Dragoon.maxTotalHealth)
      m.passive = true
      m
    }
    val pilot = new KitePilot(mate, useLeash)
    val ours = Seq(dragoon) ++ mate.toSeq
    val pilots: Map[Body, Pilot] = Map(dragoon -> (if (oldBrain) new StandPilot else pilot)) ++
      mate.map(_ -> new Pilots.Hold) ++
      Map(zealot -> new Pilots.ZombieAttack(w => w.ours, (0.0, 600.0)))
    val world = new World(ours, Seq(zealot), pilots.toMap, Seq.empty)
    var f = 0
    var lastHp = dragoon.hp
    var hits = 0
    var minDist = Double.PositiveInfinity
    while (f < frames && dragoon.alive && zealot.alive) {
      world.step()
      f += 1
      minDist = math.min(minDist, dragoon.dist(zealot))
      if (dragoon.hp < lastHp) hits += 1
      lastHp = dragoon.hp
    }
    Outcome(dragoon.alive, dragoon.hp, hits, zealot.alive, pilot.plants, pilot.yields, pilot.leashes, pilot.maxSepTiles, f, minDist)
  }

  test("a fresh Dragoon kites a lone Zealot without taking a hit") {
    println(s"DIAG speeds dragoon=${Protoss.Dragoon.topSpeed} zealot=${Protoss.Zealot.topSpeed}")
    val fixed = runDuel(useLeash = false, oldBrain = false, mateAt = None)
    println(s"DIAG fixed alive=${fixed.alive} hp=${fixed.hpEnd.toInt} hits=${fixed.hitsTaken} foeAlive=${fixed.foeAlive} plants=${fixed.plants} yields=${fixed.yields} frames=${fixed.frames} minDist=${fixed.minDistPx.toInt}px")
    assert(fixed.plants > 0, "the kite never planted a volley - it ran instead of fighting")
    assert(fixed.yields > 0, "the kite never yielded - it stood instead of kiting")
    assert( ! fixed.foeAlive, "the Zealot survived a duel it can never win")
    assert(fixed.alive, "the Dragoon died walking its own kite")
    assert(fixed.hitsTaken == 0, "the Dragoon took hits it must never take")

    val old = runDuel(useLeash = false, oldBrain = true, mateAt = None)
    println(s"DIAG old alive=${old.alive} hp=${old.hpEnd.toInt} hits=${old.hitsTaken} foeAlive=${old.foeAlive} frames=${old.frames} minDist=${old.minDistPx.toInt}px")
    assert(old.hitsTaken >= 4, "the old brain walks the blades for free - the scenario cannot punish standing")
  }

  test("a kiter past six tiles steers back toward its pack") {
    val fixed = runDuel(useLeash = true, oldBrain = false, mateAt = Some((150.0, 300.0)))
    println(s"DIAG leash alive=${fixed.alive} hits=${fixed.hitsTaken} foeAlive=${fixed.foeAlive} leashes=${fixed.leashes} maxSep=${fixed.maxSepTiles} frames=${fixed.frames} minDist=${fixed.minDistPx.toInt}px")
    assert(fixed.leashes > 0, "the leash never owned a frame - the kite ran straight past the pack")
    assert( ! fixed.foeAlive, "the leashed kite never killed its chaser")
    assert(fixed.hitsTaken == 0, "the leashed kite took hits it must never take")

    val free = runDuel(useLeash = false, oldBrain = false, mateAt = Some((150.0, 300.0)))
    println(s"DIAG free maxSep=${free.maxSepTiles} foeAlive=${free.foeAlive} frames=${free.frames} minDist=${free.minDistPx.toInt}px")
    assert(free.maxSepTiles > fixed.maxSepTiles, "the leash changes nothing - nothing to fix here")
  }
}
