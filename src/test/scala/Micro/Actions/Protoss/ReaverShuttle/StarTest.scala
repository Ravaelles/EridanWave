package Micro.Actions.Protoss.ReaverShuttle

import scala.collection.mutable.ArrayBuffer

/**
 * StarTest: a minimal frame stepper for doctrine duels (test scope only).
 * Movers hold tile positions and constant speeds; each frame ours walk
 * their goals while foes walk at us, and a foe in range with a ready
 * weapon deals its damage instantly. No pathing, no cooldown trickery -
 * just enough world to prove a maneuver takes no hits it should not.
 */
object StarTest {

  case class Mover(
    var x: Double,
    var y: Double,
    speedTiles: Double,
    var hp: Double,
    maxHp: Double,
    rangeTiles: Double,
    dmgPerShot: Double,
    cooldownFrames: Int,
    isStatic: Boolean = false,
    isBuilding: Boolean = false,
    isStaticDefense: Boolean = false,
    flees: Boolean = false) {

    var cooldownLeft: Int = 0
    var goal: Option[(Double, Double)] = None

    def alive: Boolean = hp > 0

    def wound01: Double = (maxHp - hp) / maxHp

    def hpLossFraction: Double = (maxHp - hp) / maxHp

    def distTiles(other: Mover): Double = {
      val dx = x - other.x
      val dy = y - other.y
      Math.sqrt(dx * dx + dy * dy)
    }

    def stepToward(tx: Double, ty: Double): Unit = {
      val dx = tx - x
      val dy = ty - y
      val dist = Math.sqrt(dx * dx + dy * dy)
      if (dist > 0) {
        val stride = Math.min(speedTiles, dist)
        x += dx / dist * stride
        y += dy / dist * stride
      }
    }

    def approach(tx: Double, ty: Double, stopTiles: Double): Unit = {
      // Attack-move arrival: close in, stopping at the given reach instead
      // of striding a full frame past it. Callers pass weapon reach for
      // statics with guns (a 2-tile stride would land inside them every
      // approach) and zero for mobiles (a capped approach lets anything
      // fleeing outward stalemate at the rim). Unchanged when inside reach.
      val dx = tx - x
      val dy = ty - y
      val dist = Math.sqrt(dx * dx + dy * dy)
      if (dist > stopTiles) {
        val stride = Math.min(speedTiles, dist - stopTiles)
        x += dx / dist * stride
        y += dy / dist * stride
      }
    }

    def stepAwayFrom(tx: Double, ty: Double, standTiles: Double): Unit = {
      val dx = x - tx
      val dy = y - ty
      val dist = Math.sqrt(dx * dx + dy * dy)
      if (dist > 0) {
        val stride = Math.min(speedTiles, Math.max(0.0, standTiles - dist))
        x += dx / dist * stride
        y += dy / dist * stride
      } else {
        x += Math.min(speedTiles, standTiles)
      }
    }

    def fleeFrom(tx: Double, ty: Double): Unit = {
      val dx = x - tx
      val dy = y - ty
      val dist = Math.sqrt(dx * dx + dy * dy)
      if (dist > 0) {
        x += dx / dist * speedTiles
        y += dy / dist * speedTiles
      }
    }
  }

  class World(val ours: Seq[Mover], val foes: Seq[Mover], val suicideMission: Boolean = false) {

    // Suicide targeting veto, straight from the production rule: buildings
    // the squad never asked for get no vote. The pilot calls the real
    // kernel, so a StarEngine raze proves the shipped rule, not a copy.
    private def vetoed(f: Mover): Boolean =
      f.isBuilding && ! Micro.Targeting.FiltersRequired.TargetFilterFocus.allowsBuilding(
        isBuilding = f.isBuilding,
        isStaticDefense = f.isStaticDefense,
        isSuicideMission = suicideMission)

    var frame: Int = 0
    val hits: ArrayBuffer[(Int, Double)] = ArrayBuffer.empty

    private def nearestOur(foe: Mover): Option[Mover] = {
      val live = ours.filter(_.alive)
      if (live.isEmpty) None else Some(live.minBy(foe.distTiles))
    }

    def step(): Unit = {
      ours.filter(_.alive).foreach(o => {
        nearestFoe(o) match {
          // Hold the reach advantage vs statics: backed inside their guns
          // (an escort melee drags us in), step back out to max reach and
          // keep shooting for free instead of sitting under them.
          case Some(f) if f.isStatic && o.dmgPerShot > 0 && o.rangeTiles > f.rangeTiles && o.distTiles(f) <= f.rangeTiles =>
            o.stepAwayFrom(f.x, f.y, o.rangeTiles)
          // Can hit it: hold and shoot. Cannot reach it: close in.
          // Unarmed or no contact: fly the mission goal.
          case Some(f) if o.dmgPerShot > 0 && o.rangeTiles > 0 && o.distTiles(f) <= o.rangeTiles =>
          case Some(f) if o.dmgPerShot > 0 && o.rangeTiles > 0 =>
            // Against a static with reach stop at weapon edge (landing
            // inside its guns every approach is a harness artifact, not a
            // maneuver); mobiles get full-stride pursuit, or a capped
            // approach lets anything fleeing outward stalemate at the rim.
            o.approach(f.x, f.y, if (f.isStatic && f.rangeTiles > 0) o.rangeTiles else 0.0)
          case _ => o.goal.foreach(g => o.stepToward(g._1, g._2))
        }
      })
      foes.filter(_.alive).foreach(f => nearestOur(f).foreach(n =>
        if (f.flees) f.fleeFrom(n.x, n.y) else f.stepToward(n.x, n.y)))
      foes.filter(_.alive).foreach(f => {
        if (f.cooldownLeft > 0) {
          f.cooldownLeft -= 1
        } else {
          nearestOur(f).filter(n => f.distTiles(n) <= f.rangeTiles).foreach(victim => {
            victim.hp = Math.max(0, victim.hp - f.dmgPerShot)
            hits += ((frame, f.dmgPerShot))
            f.cooldownLeft = f.cooldownFrames
          })
        }
      })
      ours.filter(_.alive).foreach(o => {
        if (o.cooldownLeft > 0) {
          o.cooldownLeft -= 1
        } else {
          nearestFoe(o).filter(f => o.dmgPerShot > 0 && o.distTiles(f) <= o.rangeTiles).foreach(victim => {
            victim.hp = Math.max(0, victim.hp - o.dmgPerShot)
            hits += ((frame, -o.dmgPerShot))
            o.cooldownLeft = o.cooldownFrames
          })
        }
      })
      frame += 1
    }

    private def nearestFoe(o: Mover): Option[Mover] = {
      val live = foes.filter(_.alive).filterNot(vetoed)
      if (live.isEmpty) None else Some(live.minBy(o.distTiles))
    }

    def run(frames: Int): Unit = {
      var remaining = frames
      while (remaining > 0 && ours.exists(_.alive)) {
        step()
        remaining -= 1
      }
    }
  }
}
